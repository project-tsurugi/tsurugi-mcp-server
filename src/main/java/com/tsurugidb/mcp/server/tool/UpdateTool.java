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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tsurugidb.mcp.server.util.ExceptionUtil;

import io.modelcontextprotocol.server.McpSyncServerExchange;

public class UpdateTool extends AbstractTool {

    @Override
    public String toolName() {
        return "update";
    }

    @Override
    protected String toolDescription() {
        return "execute SQL (insert, update, delete) in Tsurugi RDBMS";
    }

    static final String SQL = "sql";
    static final String TRANSACTION_TYPE = "transaction_type";
    static final String WRITE_PRESERVE = "write_preserve";

    @Override
    protected List<ToolProperty> properties() {
        return List.of( //
                ToolProperty.of(SQL, "SQL (insert, update, delete) to execute", true), //
                ToolProperty.of(TRANSACTION_TYPE, "transaction type. `OCC`, `LTX`. default is `OCC`", false), //
                ToolProperty.of(WRITE_PRESERVE, "table names for target (comma separate). Required when transaction_type is LTX", false) //
        );
    }

    @Override
    protected Object action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception {
        String sql = (String) arguments.get(SQL);
        String transactionType = (String) arguments.get(TRANSACTION_TYPE);
        List<String> writePreserve = null;
        {
            String wp = (String) arguments.get(WRITE_PRESERVE);
            if (wp != null) {
                writePreserve = Arrays.stream(wp.split(",")).map(String::trim).toList();
            }
        }

        try (var session = pool.getSession()) {
            return session.executeStatement(sql, transactionType, writePreserve);
        } catch (Exception e) {
            LOG.warn("Failed to execute update", e);
            return ExceptionUtil.createErrorToolResult(e);
        }
    }
}
