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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.tool.TableNamesTool.TableName;

class ListTableNamesToolTest extends TsurugiMcpTester {

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void action(TsurugiMode mode) throws Exception {
        var arguments = createTestArguments(mode);
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                session.executeDdl("drop table if exists mcp_test", "OCC");
                session.executeDdl("create table mcp_test (pk int primary key)", "OCC");
            }

            var target = new TableNamesTool();
            target.initialize(createJsonMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            @SuppressWarnings("unchecked")
            var result = (List<TableName>) target.action(null, args);

            assertTrue(result.stream().anyMatch(t -> t.tableName().equals("mcp_test")));
        }
    }
}
