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
package com.tsurugidb.mcp.server.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.QueryUtil.QueryResult;
import com.tsurugidb.mcp.server.dao.SessionPool;

class QueryToolTest extends TsurugiMcpTester {

    private static final int SIZE = 500;

    @BeforeAll
    static void beforeAll() throws Exception {
        var arguments = createTestArguments(TsurugiMode.ICEAXE);
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                session.executeDdl("drop table if exists customer", "OCC");
                session.executeDdl("""
                        create table customer (
                          c_id bigint primary key,
                          c_name varchar(20),
                          c_age int,
                          c_date date
                        )
                        """, "OCC");
                for (int i = 1; i <= SIZE; i++) {
                    long id = i;
                    String name = "name" + i;
                    int age = i % 100 + 1;
                    String date = LocalDate.now().toString();
                    String sql = String.format("insert into customer values(%d, '%s', %d, '%s')", id, name, age, date);

                    session.executeStatement(sql, "OCC", null);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action(TsurugiMode mode) throws Exception {
        action(mode, null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_OCC(TsurugiMode mode) throws Exception {
        action(mode, "OCC");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_LTX(TsurugiMode mode) throws Exception {
        action(mode, "LTX");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_RTX(TsurugiMode mode) throws Exception {
        action(mode, "RTX");
    }

    private void action(TsurugiMode mode, String transactionType) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            var target = new QueryTool();
            target.initialize(createJsonMapper(), arguments, pool);

            String cursor;
            var list = new ArrayList<Map<String, Object>>(SIZE);
            {
                var args = new HashMap<String, Object>();
                args.put(QueryTool.SQL, "select * from customer order by c_id");
                if (transactionType != null) {
                    args.put(QueryTool.TRANSACTION_TYPE, transactionType);
                }
                var result = (QueryResult) target.action(null, args);

                list.addAll(result.rows());
                assertNull(result.serializationFailureMessage());
                assertTrue(result.hasMoreRecord());
                cursor = result.nextCursor();
                assertNotNull(cursor);
            }

            while (cursor != null) {
                var args = new HashMap<String, Object>();
                args.put(QueryTool.CURSOR, cursor);
                var result = (QueryResult) target.action(null, args);

                list.addAll(result.rows());
                assertNull(result.serializationFailureMessage());
                cursor = result.nextCursor();
                if (result.hasMoreRecord()) {
                    assertNotNull(cursor);
                } else {
                    assertNull(cursor);
                }
            }

            assertEquals(SIZE, list.size());
            int i = 1;
            for (var map : list) {
                assertEquals((long) i, map.get("c_id"));
                assertEquals("name" + i, map.get("c_name"));
                assertEquals(i % 100 + 1, map.get("c_age"));
                i++;
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_serializationFailure(TsurugiMode mode) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession(); //
                    var ltx = session.createTransaction("LTX", List.of("customer"))) {

                try {
                    var target = new QueryTool();
                    target.initialize(createJsonMapper(), arguments, pool);

                    var args = new HashMap<String, Object>();
                    args.put(QueryTool.SQL, "select * from customer order by c_id");
                    args.put(QueryTool.TRANSACTION_TYPE, "OCC");
                    var result = (QueryResult) target.action(null, args);

                    assertNotNull(result.serializationFailureMessage());
                    assertTrue(result.serializationFailureMessage().contains("CC_EXCEPTION"));
                    assertFalse(result.hasMoreRecord());
                    assertNull(result.nextCursor());
                } finally {
                    ltx.rollback();
                }
            }
        }
    }
}
