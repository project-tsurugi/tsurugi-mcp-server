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
import java.util.Map;

import com.tsurugidb.iceaxe.sql.TsurugiSqlQuery;
import com.tsurugidb.iceaxe.sql.result.TsurugiQueryResult;
import com.tsurugidb.iceaxe.sql.result.TsurugiResultRecord;
import com.tsurugidb.iceaxe.sql.type.TgBlobReference;
import com.tsurugidb.iceaxe.sql.type.TgClobReference;
import com.tsurugidb.iceaxe.transaction.TgCommitType;
import com.tsurugidb.iceaxe.transaction.TsurugiTransaction;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;

public class IceaxeResultSet implements TsurugiMcpResultSet {
    private final TsurugiTransaction transaction;
    private final TsurugiSqlQuery<TsurugiResultRecord> ps;
    private final TsurugiQueryResult<TsurugiResultRecord> queryResult;

    public IceaxeResultSet(TsurugiTransaction transaction, TsurugiSqlQuery<TsurugiResultRecord> ps, TsurugiQueryResult<TsurugiResultRecord> queryResult) {
        this.transaction = transaction;
        this.ps = ps;
        this.queryResult = queryResult;
    }

    @Override
    public Map<String, Object> nextRow() throws IOException, InterruptedException, TsurugiTransactionException {
        var recordOpt = queryResult.findRecord();
        if (recordOpt.isEmpty()) {
            return null;
        }

        var record = recordOpt.get();
        var nameList = record.getNameList();
        var map = new LinkedHashMap<String, Object>(nameList.size());
        for (String name : nameList) {
            Object value = convert(record.nextValueOrNull());
            map.put(name, value);
        }
        return map;
    }

    Object convert(Object value) throws IOException, InterruptedException, TsurugiTransactionException {
        if (value instanceof TgBlobReference blob) {
            return blob.readAllBytes();
        }
        if (value instanceof TgClobReference clob) {
            return clob.readString();
        }

        return value;
    }

    @Override
    public void commit() throws IOException, InterruptedException, TsurugiTransactionException {
        transaction.commit(TgCommitType.DEFAULT);
    }

    @Override
    public void close() throws java.io.IOException, InterruptedException, TsurugiTransactionException {
        try (transaction; ps; queryResult) {
            // close only
        }
    }
}
