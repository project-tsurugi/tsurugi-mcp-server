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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tsurugidb.grpc.client.exception.ServerException;
import com.tsurugidb.grpc.client.exception.code.SqlDiagnosticCode;
import com.tsurugidb.grpc.client.metadata.Name;
import com.tsurugidb.grpc.client.session.Session;
import com.tsurugidb.grpc.client.sql.SqlClient;
import com.tsurugidb.grpc.client.sql.query.QueryOption;
import com.tsurugidb.grpc.client.sql.query.QueryResult;
import com.tsurugidb.grpc.client.sql.query.ResultSet;
import com.tsurugidb.grpc.client.sql.query.TimestampTimeUnit;
import com.tsurugidb.grpc.client.transaction.TransactionOption;
import com.tsurugidb.grpc.client.transaction.TransactionType;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;
import com.tsurugidb.mcp.server.dao.TsurugiMcpSession;
import com.tsurugidb.mcp.server.dao.TsurugiMcpTransaction;
import com.tsurugidb.mcp.server.entity.TableMetadata;

public class GrpcSession extends TsurugiMcpSession {

    private final Session session;
    private final SqlClient sqlClient;

    public GrpcSession(SessionPool pool, Session session) {
        super(pool);
        this.session = session;
        this.sqlClient = SqlClient.attach(session);
    }

    @Override
    public void keepAlive() throws IOException, InterruptedException, ServerException {
        session.keepAlive();
    }

    @Override
    public List<String> getTableNameList() throws IOException, InterruptedException, ServerException {
        List<Name> tableNames = sqlClient.listTables();
        return tableNames.stream().map(Name::toString).toList();
    }

    @Override
    public TableMetadata getTableMetadata(String tableName) throws IOException, InterruptedException, ServerException {
        try {
            var metadata = sqlClient.getTableMetadata(tableName);
            return TableMetadata.of(metadata);
        } catch (ServerException e) {
            if (e.getDiagnosticCode() == SqlDiagnosticCode.TARGET_NOT_FOUND_EXCEPTION) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public TsurugiMcpTransaction createTransaction(String transactionType, List<String> writePreserve) throws IOException, InterruptedException, ServerException {
        var txOption = getTransactionOption(transactionType, writePreserve);
        var transaction = sqlClient.createTransaction(txOption);

        return new GrpcTransaction(sqlClient, transaction);
    }

    @Override
    public void executeDdl(String sql, String transactionType) throws IOException, InterruptedException, ServerException {
        var txOption = getDdlTransactionOption(transactionType);

        try (var transaction = sqlClient.createTransaction(txOption)) {
            sqlClient.execute(transaction, sql);
            sqlClient.commit(transaction);
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
    public Map<String, Long> executeStatement(String sql, String transactionType, List<String> writePreserve) throws IOException, InterruptedException, ServerException {
        var txOption = getTransactionOption(transactionType, writePreserve);

        var result = new LinkedHashMap<String, Long>();
        try (var transaction = sqlClient.createTransaction(txOption)) {
            var count = sqlClient.execute(transaction, sql);
            for (var entry : count.getCounterMap().entrySet()) {
                result.put(entry.getKey().name().toLowerCase(), entry.getValue());
            }

            sqlClient.commit(transaction);
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
    public TsurugiMcpResultSet executeQuery(String sql, String transactionType) throws IOException, InterruptedException, ServerException {
        var txOption = getQueryTransactionOption(transactionType);

        var transaction = sqlClient.createTransaction(txOption);
        QueryResult queryResult;
        try {
            var queryOption = QueryOption.newBuilder() //
                    .timestampTimeUnit(TimestampTimeUnit.TSURUGI) //
                    .recordBatchInBytes(1024 * 1024) // 1MiB
                    .build();
            queryResult = sqlClient.query(transaction, sql, queryOption);
        } catch (Exception e) {
            try {
                transaction.close();
            } catch (Exception s) {
                e.addSuppressed(s);
            }
            throw e;
        }

        var resultSet = ResultSet.from(queryResult);
        return new GrpcResultSet(sqlClient, transaction, resultSet);
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
    public void actualClose() throws IOException, InterruptedException, ServerException {
        session.close();
    }
}
