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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.SessionPool;

class UpdateToolTest extends TsurugiMcpTester {

    @BeforeAll
    static void beforeAll() throws Exception {
        var arguments = createTestArguments(TsurugiMode.ICEAXE);
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                session.executeDdl("drop table if exists mcp_example", "OCC");
                session.executeDdl("""
                        create table mcp_example (
                          pk int primary key,
                          value bigint
                        )
                        """, "OCC");
                session.executeStatement("insert into mcp_example values(1, 11)", "OCC", null);
                session.executeStatement("insert into mcp_example values(2, 22)", "OCC", null);
                session.executeStatement("insert into mcp_example values(3, 33)", "OCC", null);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action(TsurugiMode mode) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createJsonMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=111 where pk=1");
            @SuppressWarnings("unchecked")
            var result = (Map<String, Long>) target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(mode, 1, 111);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_OCC(TsurugiMode mode) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createJsonMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=222 where pk=2");
            args.put(UpdateTool.TRANSACTION_TYPE, "OCC");
            @SuppressWarnings("unchecked")
            var result = (Map<String, Long>) target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(mode, 2, 222);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action_LTX(TsurugiMode mode) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            var target = new UpdateTool();
            target.initialize(createJsonMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(UpdateTool.SQL, "update mcp_example set value=333 where pk=3");
            args.put(UpdateTool.TRANSACTION_TYPE, "LTX");
            args.put(UpdateTool.WRITE_PRESERVE, "mcp_example");
            @SuppressWarnings("unchecked")
            var result = (Map<String, Long>) target.action(null, args);

            assertEquals(Map.of("updated_rows", 1L), result);
            assertSelect(mode, 3, 333);
        }
    }

    private static void assertSelect(TsurugiMode mode, int pk, int value) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                int count = 0;
                try (var rs = session.executeQuery("select * from mcp_example where pk=" + pk, "OCC")) {
                    var row = rs.nextRow();
                    long actualValue = (Long) row.get("value");
                    assertEquals(value, actualValue);
                    count++;
                }
                assertEquals(1, count);
            }
        }
    }
}
