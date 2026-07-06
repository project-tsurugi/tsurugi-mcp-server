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
package com.tsurugidb.mcp.server.dao;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.mcp.server.entity.TableMetadata;

public abstract class TsurugiMcpSession implements AutoCloseable {

    private final SessionPool ownerPool;

    public TsurugiMcpSession(SessionPool pool) {
        this.ownerPool = pool;
    }
    
    public abstract void keepAlive() throws IOException, InterruptedException;

    public abstract List<String> getTableNameList() throws IOException, InterruptedException;

    public abstract @Nullable TableMetadata getTableMetadata(String tableName) throws IOException, InterruptedException;

    public abstract TsurugiMcpTransaction createTransaction(String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException;

    public abstract void executeDdl(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException;

    public abstract Map<String, Long> executeStatement(String sql, String transactionType, List<String> writePreserve) throws IOException, InterruptedException, TsurugiTransactionException;

    public abstract TsurugiMcpResultSet executeQuery(String sql, String transactionType) throws IOException, InterruptedException, TsurugiTransactionException;

    @Override
    public final void close() {
        ownerPool.returnSession(this);
    }

    public abstract void actualClose() throws IOException, InterruptedException;
}
