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
package com.tsurugidb.mcp.server.dao.iceaxe;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.iceaxe.session.TgSessionOption.TgTimeoutKey;
import com.tsurugidb.iceaxe.session.TsurugiSession;
import com.tsurugidb.iceaxe.sql.result.TgResultMapping;
import com.tsurugidb.iceaxe.transaction.TgCommitType;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;
import com.tsurugidb.mcp.server.dao.TsurugiMcpSession;
import com.tsurugidb.mcp.server.dao.TsurugiMcpTransaction;
import com.tsurugidb.mcp.server.entity.TableMetadata;
import com.tsurugidb.tsubakuro.exception.ServerException;

public class IceaxeSession extends TsurugiMcpSession {
    private final TsurugiSession session;

    public IceaxeSession(SessionPool pool, TsurugiSession session) {
        super(pool);
        this.session = session;
    }

    @Override
    public void keepAlive() throws IOException, InterruptedException {
        try (var future = session.getLowSession().updateExpirationTime()) {
            var timeout = session.getSessionOption().getTimeout(TgTimeoutKey.DEFAULT);
            future.await(timeout.value(), timeout.unit());
        } catch (ServerException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<String> getTableNameList() throws IOException, InterruptedException {
        return session.getTableNameList();
    }

    @Override
    public TableMetadata getTableMetadata(String tableName) throws IOException, InterruptedException {
        var opt = session.findTableMetadata(tableName);
        if (opt.isEmpty()) {
            return null;
        }
        return TableMetadata.of(opt.get());
    }

    @Override
    public TsurugiMcpTransaction createTransaction(String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getTransactionOption(transactionType, writePreserve);
        var transaction = session.createTransaction(txOption);
        transaction.getLowTransaction();

        return new IceaxeTransaction(transaction);
    }

    @Override
    public void executeDdl(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getDdlTransactionOption(transactionType);

        try (var transaction = session.createTransaction(txOption); //
                var ps = session.createStatement(sql)) {
            transaction.executeAndGetCountDetail(ps);
            transaction.commit(TgCommitType.DEFAULT);
        }
    }

    TgTxOption getDdlTransactionOption(String transactionType) {
        if (transactionType == null) {
            return TgTxOption.ofOCC();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> TgTxOption.ofDDL();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public Map<String, Long> executeStatement(String sql, String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getTransactionOption(transactionType, writePreserve);

        var result = new LinkedHashMap<String, Long>();
        try (var transaction = session.createTransaction(txOption); //
                var ps = session.createStatement(sql)) {
            var count = transaction.executeAndGetCountDetail(ps);
            for (var entry : count.getLowCounterMap().entrySet()) {
                result.put(entry.getKey().name().toLowerCase(), entry.getValue());
            }

            transaction.commit(TgCommitType.DEFAULT);
        }
        return result;
    }

    TgTxOption getTransactionOption(String transactionType, List<String> writePreserve) {
        if (transactionType == null) {
            return TgTxOption.ofOCC();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> {
            if (writePreserve == null) {
                yield TgTxOption.ofLTX();
            }
            yield TgTxOption.ofLTX(writePreserve);
        }
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public TsurugiMcpResultSet executeQuery(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException {
        var txOption = getQueryTransactionOption(transactionType);

        var transaction = session.createTransaction(txOption);
        var ps = session.createQuery(sql, TgResultMapping.of(record -> record));
        var queryResult = transaction.executeQuery(ps);

        return new IceaxeResultSet(transaction, ps, queryResult);
    }

    TgTxOption getQueryTransactionOption(String transactionType) {
        if (transactionType == null) {
            return TgTxOption.ofRTX();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> TgTxOption.ofLTX();
        case "RTX", "READ ONLY" -> TgTxOption.ofRTX();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }

    @Override
    public void actualClose() throws IOException, InterruptedException {
        session.close();
    }
}
