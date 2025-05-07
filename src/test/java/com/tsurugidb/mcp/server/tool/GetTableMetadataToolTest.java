/*
 * Copyright 2025 Project Tsurugi.
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
package com.tsurugidb.mcp.server.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.entity.TableMetadata;

class GetTableMetadataToolTest extends ToolTester {

    @Test
    void action() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager();
                tm.executeDdl("drop table if exists customer");
                tm.executeDdl("""
                        /**
                         customer for MCP test.
                         */
                        create table customer (
                          /** customer id */
                          c_id bigint primary key,
                          /** customer name */
                          c_name varchar(20),
                          /** customer age */
                          c_age int
                        )
                        """);
            }

            var target = new TableMetadataTool();
            target.initialize(createObjectMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(TableMetadataTool.TABLE_NAME, "customer");
            var result = (TableMetadata) target.action(null, args);

            assertEquals("customer", result.tableName());
            assertEquals("customer for MCP test.", result.tableDescription());

            var columns = result.columns();
            assertEquals(3, columns.size());
            int i = 0;
            {
                var column = columns.get(i++);
                assertEquals("c_id", column.columnName());
                assertEquals("customer id", column.columnDescription());
                assertEquals("BIGINT", column.columnType());
                assertEquals("NOT NULL", column.constraint());
            }
            {
                var column = columns.get(i++);
                assertEquals("c_name", column.columnName());
                assertEquals("customer name", column.columnDescription());
                assertEquals("VARCHAR(20)", column.columnType());
                assertEquals("NULL", column.constraint());
            }
            {
                var column = columns.get(i++);
                assertEquals("c_age", column.columnName());
                assertEquals("customer age", column.columnDescription());
                assertEquals("INT", column.columnType());
                assertEquals("NULL", column.constraint());
            }
        }
    }
}
