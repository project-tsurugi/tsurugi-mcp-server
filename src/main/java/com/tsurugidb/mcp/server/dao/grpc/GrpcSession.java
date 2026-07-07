/*
 * Copyright 2025-2026 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.mcp.server.dao.grpc;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tsurugidb.grpc.client.metadata.Name;
import com.tsurugidb.grpc.client.session.Session;
import com.tsurugidb.grpc.client.sql.query.QueryOption;
import com.tsurugidb.grpc.client.sql.query.TimestampTimeUnit;
import com.tsurugidb.grpc.client.transaction.CommitOption;
import com.tsurugidb.grpc.client.transaction.TransactionOption;
import com.tsurugidb.grpc.client.transaction.TransactionType;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;
import com.tsurugidb.mcp.server.dao.TsurugiMcpSession;
import com.tsurugidb.mcp.server.dao.TsurugiMcpTransaction;
import com.tsurugidb.mcp.server.entity.TableMetadata;

public class GrpcSession extends TsurugiMcpSession {
    private final Session session;
    private Duration timeout = Duration.ZERO;

    public GrpcSession(SessionPool pool, Session session) {
        super(pool);
        this.session = session;
    }

    @Override
    public void keepAlive() throws IOException, InterruptedException {
        session.keepAlive(timeout);
    }

    @Override
    public List<String> getTableNameList() throws IOException, InterruptedException {
        var helper = session.getMetadataHelper();
        List<Name> tableNames = helper.listTables(timeout);
        return tableNames.stream().map(Name::toString).toList();
    }

    @Override
    public TableMetadata getTableMetadata(String tableName) throws IOException, InterruptedException {
        var helper = session.getMetadataHelper();
        var metadata = helper.getTableMetadata(tableName, timeout);
        if (metadata == null) {
            return null;
        }

        return TableMetadata.of(metadata);
    }

    @Override
    public TsurugiMcpTransaction createTransaction(String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getTransactionOption(transactionType, writePreserve);
        var transaction = session.createTransaction(txOption, timeout);

        return new GrpcTransaction(transaction);
    }

    @Override
    public void executeDdl(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getDdlTransactionOption(transactionType);

        try (var transaction = session.createTransaction(txOption, timeout)) {
            var helper = transaction.getSqlHelper();
            helper.executeStatement(sql, timeout);

            var option = CommitOption.newBuilder().build();
            transaction.commit(option, timeout);
        }
    }

    TransactionOption getDdlTransactionOption(String transactionType) {
        if (transactionType == null) {
            return TransactionOption.newBuilder().transactionType(TransactionType.OCC).build();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TransactionOption.newBuilder().transactionType(TransactionType.OCC).build();
        case "LTX", "LONG" -> TransactionOption.newBuilder().transactionType(TransactionType.LTX).includeDdl(true).build();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public Map<String, Long> executeStatement(String sql, String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getTransactionOption(transactionType, writePreserve);

        var result = new LinkedHashMap<String, Long>();
        try (var transaction = session.createTransaction(txOption, timeout)) {
            var helper = transaction.getSqlHelper();
            var count = helper.executeStatement(sql, timeout);
            for (var entry : count.getCounterMap().entrySet()) {
                result.put(entry.getKey().name().toLowerCase(), entry.getValue());
            }

            var option = CommitOption.newBuilder().build();
            transaction.commit(option, timeout);
        }
        return result;
    }

    TransactionOption getTransactionOption(String transactionType, List<String> writePreserve) {
        if (transactionType == null) {
            return TransactionOption.newBuilder().transactionType(TransactionType.OCC).build();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TransactionOption.newBuilder().transactionType(TransactionType.OCC).build();
        case "LTX", "LONG" -> {
            if (writePreserve == null) {
                yield TransactionOption.newBuilder().transactionType(TransactionType.LTX).build();
            }
            yield TransactionOption.newBuilder().transactionType(TransactionType.LTX).writePreserve(writePreserve).build();
        }
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public TsurugiMcpResultSet executeQuery(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getQueryTransactionOption(transactionType);

        var transaction = session.createTransaction(txOption, timeout);
        var helper = transaction.getSqlHelper();

        var queryOption = QueryOption.newBuilder() //
                .timestampTimeUnit(TimestampTimeUnit.TSURUGI) //
                .recordBatchInBytes(1024 * 1024) // 1MiB
                .build();
        var quertResult = helper.executeQuery(sql, queryOption, timeout);
        var i = quertResult.getRecordIterable(timeout);

        return new GrpcResultSet(transaction, quertResult, i);
    }

    TransactionOption getQueryTransactionOption(String transactionType) {
        if (transactionType == null) {
            return TransactionOption.newBuilder().transactionType(TransactionType.RTX).build();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TransactionOption.newBuilder().transactionType(TransactionType.OCC).build();
        case "LTX", "LONG" -> TransactionOption.newBuilder().transactionType(TransactionType.LTX).build();
        case "RTX", "READ ONLY" -> TransactionOption.newBuilder().transactionType(TransactionType.RTX).build();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public void actualClose() throws IOException, InterruptedException {
        session.close();
    }
}
