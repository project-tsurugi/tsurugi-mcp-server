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
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tsurugidb.grpc.client.exception.ServerException;
import com.tsurugidb.grpc.client.sql.SqlClient;
import com.tsurugidb.grpc.client.sql.query.QueryRecord;
import com.tsurugidb.grpc.client.sql.query.ResultSet;
import com.tsurugidb.grpc.client.transaction.Transaction;
import com.tsurugidb.mcp.server.dao.TsurugiMcpResultSet;

public class GrpcResultSet implements TsurugiMcpResultSet {

    private final SqlClient sqlClient;
    private final Transaction transaction;
    private final ResultSet resultSet;

    public GrpcResultSet(SqlClient sqlClient, Transaction transaction, ResultSet resultSet) {
        this.sqlClient = sqlClient;
        this.transaction = transaction;
        this.resultSet = resultSet;
    }

    @Override
    public Map<String, Object> nextRow() throws IOException, InterruptedException, ServerException {
        if (!resultSet.next()) {
            return null;
        }

        var record = resultSet.getCurrent();
        var columnList = resultSet.getMetadata().columns();
        var map = new LinkedHashMap<String, Object>(columnList.size());
        int i = 0;
        for (var column : columnList) {
            String name = column.name();
            if (name == null || name.isEmpty()) {
                name = "@#" + i;
            }
            var type = column.sqlType();
            Object value = switch (type) {
            case BLOB -> value = downloadBlob(record, i);
            case CLOB -> value = downloadClob(record, i);
            default -> record.getValueOrNull(i);
            };
            map.put(name, value);
            i++;
        }
        return map;
    }

    private byte[] downloadBlob(QueryRecord record, int i) throws IOException, InterruptedException, ServerException {
        var blob = record.getBlobReferenceOrNull(i);
        if (blob == null) {
            return null;
        }

        try (var is = sqlClient.downloadBlob(transaction, blob)) {
            return is.readAllBytes();
        }
    }

    private String downloadClob(QueryRecord record, int i) throws IOException, InterruptedException, ServerException {
        var clob = record.getClobReferenceOrNull(i);
        if (clob == null) {
            return null;
        }

        try (var writer = new StringWriter()) {
            sqlClient.downloadClob(transaction, clob, writer);
            return writer.toString();
        }
    }

    @Override
    public void commit() throws IOException, InterruptedException, ServerException {
        sqlClient.commit(transaction);
    }

    @Override
    public void close() throws IOException, InterruptedException, ServerException {
        try (transaction; resultSet) {
            // close only
        }
    }
}
