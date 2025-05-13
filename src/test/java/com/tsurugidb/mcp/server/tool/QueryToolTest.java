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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tsurugidb.iceaxe.sql.parameter.TgBindParameters;
import com.tsurugidb.iceaxe.sql.parameter.TgBindVariables;
import com.tsurugidb.iceaxe.sql.parameter.TgParameterMapping;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.mcp.server.dao.QueryUtil.QueryResult;
import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.SessionPool;

class QueryToolTest extends TsurugiMcpTester {

    private static final int SIZE = 500;

    @BeforeAll
    static void beforeAll() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager(TgTxOption.ofOCC());
                tm.executeDdl("drop table if exists customer");
                tm.executeDdl("""
                        create table customer (
                          c_id bigint primary key,
                          c_name varchar(20),
                          c_age int,
                          c_date date
                        )
                        """);
                tm.execute(transaction -> {
                    String sql = "insert into customer values(:id, :name, :age, :date)";
                    var parameterMapping = TgParameterMapping.of(TgBindVariables.of().addLong("id").addString("name").addInt("age").addDate("date"));
                    try (var ps = session.createStatement(sql, parameterMapping)) {
                        for (int i = 1; i <= SIZE; i++) {
                            var parameter = TgBindParameters.of().add("id", (long) i).add("name", "name" + i).add("age", i % 100 + 1).add("date", LocalDate.now());
                            transaction.executeAndGetCount(ps, parameter);
                        }
                    }
                    return;
                });
            }
        }
    }

    @Test
    void action() throws Exception {
        action(null);
    }

    @Test
    void action_OCC() throws Exception {
        action("OCC");
    }

    @Test
    void action_LTX() throws Exception {
        action("LTX");
    }

    @Test
    void action_RTX() throws Exception {
        action("RTX");
    }

    private void action(String transactionType) throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            var target = new QueryTool();
            target.initialize(createObjectMapper(), arguments, pool);

            String cursor;
            var list = new ArrayList<Map<String, Object>>(SIZE);
            {
                var args = new HashMap<String, Object>();
                args.put(QueryTool.SQL, "select * from customer order by c_id");
                if (transactionType != null) {
                    args.put(QueryTool.TRANSACTION_TYPE, transactionType);
                }
                QueryResult result = target.action(null, args);

                list.addAll(result.rows());
                assertNull(result.serializationFailureMessage());
                assertTrue(result.hasMoreRecord());
                cursor = result.nextCursor();
                assertNotNull(cursor);
            }

            while (cursor != null) {
                var args = new HashMap<String, Object>();
                args.put(QueryTool.CURSOR, cursor);
                QueryResult result = target.action(null, args);

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

    @Test
    void action_serializationFailure() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession(); //
                    var ltx = session.createTransaction(TgTxOption.ofLTX("customer"))) {
                ltx.getLowTransaction();

                try {
                    var target = new QueryTool();
                    target.initialize(createObjectMapper(), arguments, pool);

                    var args = new HashMap<String, Object>();
                    args.put(QueryTool.SQL, "select * from customer order by c_id");
                    args.put(QueryTool.TRANSACTION_TYPE, "OCC");
                    QueryResult result = target.action(null, args);

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
