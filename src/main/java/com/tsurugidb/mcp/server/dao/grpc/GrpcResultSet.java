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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tsurugidb.grpc.client.sql.query.QueryRecord;
import com.tsurugidb.grpc.client.sql.query.QueryResult;
import com.tsurugidb.grpc.client.sql.query.QueryResultRecordIterable;
import com.tsurugidb.grpc.client.transaction.CommitOption;
import com.tsurugidb.grpc.client.transaction.Transaction;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;

public class GrpcResultSet implements TsurugiMcpResultSet {
    private final Transaction transaction;
    private final QueryResult queryResult;
    private final Iterator<QueryRecord> recordIterator;
    private Duration timeout;

    public GrpcResultSet(Transaction transaction, QueryResult queryResult, QueryResultRecordIterable iterable) {
        this.transaction = transaction;
        this.queryResult = queryResult;
        this.recordIterator = iterable.iterator();
    }

    @Override
    public Map<String, Object> nextRow() throws IOException, InterruptedException, TsurugiTransactionException {
        if (!recordIterator.hasNext()) {
            return null;
        }

        var record = recordIterator.next();
        var nameList = record.getNameList();
        var typeList = record.getSqlTypeList();
        var map = new LinkedHashMap<String, Object>(nameList.size());
        int i = 0;
        for (String name : nameList) {
            var type = typeList.get(i);
            Object value = switch (type) {
            case TIME_WITH_TIME_ZONE -> value = record.getTimeTzOrNullValue(i);
            case TIMESTAMP_WITH_TIME_ZONE -> value = record.getTimestampTzOrNullValue(i);
            case BLOB -> value = record.getBlobOrNullValue(i, null);
            case CLOB -> value = record.getClobOrNullValue(i, null);
            default -> record.getValue(i);
            };
            map.put(name, value);
            i++;
        }
        return map;
    }

    @Override
    public void commit() throws IOException, InterruptedException, TsurugiTransactionException {
        var option = CommitOption.newBuilder().build();
        transaction.commit(option, timeout);
    }

    @Override
    public void close() throws java.io.IOException, InterruptedException, TsurugiTransactionException {
        try (transaction; queryResult) {
            // close only
        }
    }
}
