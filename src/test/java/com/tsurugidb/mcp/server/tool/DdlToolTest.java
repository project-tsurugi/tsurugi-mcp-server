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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.SessionPool;

class DdlToolTest extends TsurugiMcpTester {

    @BeforeEach
    void beforeEach() throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            try (var session = pool.getSession()) {
                var tm = session.createTransactionManager(TgTxOption.ofOCC());
                tm.executeDdl("drop table if exists mcp_example");
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

    private void action(String transactionType) throws Exception {
        var arguments = createTestArguments();
        try (var pool = SessionPool.create(arguments)) {
            var target = new DdlTool();
            target.initialize(createObjectMapper(), arguments, pool);

            var args = new HashMap<String, Object>();
            args.put(DdlTool.SQL, "create table mcp_example (pk int primary key)");
            if (transactionType != null) {
                args.put(DdlTool.TRANSACTION_TYPE, transactionType);
            }
            String result = target.action(null, args);

            assertEquals("succeeded", result);
        }
    }
}
