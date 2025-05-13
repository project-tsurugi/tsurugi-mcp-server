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
package com.tsurugidb.mcp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.mcp.server.dao.SessionPool;

class TsurugiMcpResourceTest extends TsurugiMcpTester {

    @Test
    void tableSchema() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager();
                tm.executeDdl("drop table if exists r_customer");
                tm.executeDdl("""
                        /**
                         customer for MCP test.
                         */
                        create table r_customer (
                          /** customer id */
                          c_id bigint primary key,
                          /** customer name */
                          c_name varchar(20),
                          /** customer age */
                          c_age int
                        )
                        """);
            }

            var target = new TsurugiMcpResource(createObjectMapper(), arguments, pool);

            var result = target.tableSchemaMain("tsurugidb://r_customer/schema");

            assertEquals("r_customer", result.tableName());
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
