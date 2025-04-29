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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import com.tsurugidb.mcp.server.entity.TableMetadata;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class TableMetadataTool extends AbstractTool {

    @Override
    public String toolName() {
        return "getTableMetadata";
    }

    @Override
    protected String toolDescription() {
        return "get table metadata (table, column schema) from Tsurugi RDBMS";
    }

    static final String TABLE_NAME = "tableName";

    @Override
    protected List<ToolProperty> properties() {
        return List.of(ToolProperty.of(TABLE_NAME, "table name", true));
    }

    @Override
    protected Object action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception {
        String tableName = (String) arguments.get(TABLE_NAME);

        try (var session = pool.getSession()) {
            var opt = session.findTableMetadata(tableName);
            if (opt.isEmpty()) {
                var content = new McpSchema.TextContent(MessageFormat.format("table not found. specified table name: {0}", tableName));
                return new CallToolResult(List.of(content), true);
            }
            var metadata = TableMetadata.of(opt.get());

            return metadata;
        }
    }
}
