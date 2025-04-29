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
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.mcp.server.dao.SessionPool;

class UpdateToolTest extends ToolTester {

    @BeforeAll
    static void beforeAll() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager(TgTxOption.ofOCC());
                tm.executeDdl("drop table if exists mcp_example");
                tm.executeDdl("""
                        create table mcp_example (
                          pk int primary key,
                          value bigint
                        )
                        """);
                tm.executeAndGetCount("insert into mcp_example values(1, 11)");
                tm.executeAndGetCount("insert into mcp_example values(2, 22)");
                tm.executeAndGetCount("insert into mcp_example values(3, 33)");
            }
        }
    }

    @Test
    void action() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createObjectMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=111 where pk=1");
            Map<String, Long> result = target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(1, 111);
        }
    }

    @Test
    void action_OCC() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createObjectMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=222 where pk=2");
            args.put(UpdateTool.TRANSACTION_TYPE, "OCC");
            Map<String, Long> result = target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(2, 222);
        }
    }

    @Test
    void action_LTX() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createObjectMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=333 where pk=3");
            args.put(UpdateTool.TRANSACTION_TYPE, "LTX");
            args.put(UpdateTool.WRITE_PRESERVE, "mcp_example");
            Map<String, Long> result = target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(3, 333);
        }
    }

    private static void assertSelect(int pk, int value) throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager(TgTxOption.ofOCC());
                var entityList = tm.executeAndGetList("select * from mcp_example where pk=" + pk);
                assertEquals(1, entityList.size());
                var entity = entityList.getFirst();
                assertEquals(value, entity.getIntOrNull("value"));
            }
        }
    }
}
