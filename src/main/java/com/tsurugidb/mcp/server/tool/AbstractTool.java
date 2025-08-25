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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.dao.SessionPool;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public abstract class AbstractTool {

    protected ObjectMapper objectMapper;
    protected SessionPool pool;

    public void initialize(ObjectMapper objectMapper, Arguments arguments, SessionPool pool) {
        this.objectMapper = objectMapper;
        this.pool = pool;
    }

    public SyncToolSpecification syncTool() {
        String toolName = toolName();
        String description = toolDescription();

        List<ToolProperty> defineList = properties();
        var properties = new LinkedHashMap<String, Object>(defineList.size());
        var requiredProperties = new ArrayList<String>();
        for (var property : defineList) {
            String name = property.name();
            properties.put(name, property.body());

            if (property.required()) {
                requiredProperties.add(name);
            }
        }

        var schema = new McpSchema.JsonSchema( //
                "object", // type
                properties, // properties
                requiredProperties, // required property
                null, // additionalProperties
                null, null);

        var tool = McpSchema.Tool.builder() //
                .name(toolName) //
                .description(description) //
                .inputSchema(schema) //
                .build();
        return SyncToolSpecification.builder() //
                .tool(tool) //
                .callHandler(this::caller) //
                .build();
    }

    public abstract String toolName();

    protected abstract String toolDescription();

    protected record ToolProperty(String name, ToolPropertyBody body, boolean required) {
        public static ToolProperty of(String name, String description, boolean required) {
            return new ToolProperty(name, ToolPropertyBody.ofString(description), required);
        }
    }

    protected record ToolPropertyBody(String type, String description) {
        public static ToolPropertyBody ofString(String description) {
            return new ToolPropertyBody("string", description);
        }
    }

    protected abstract List<ToolProperty> properties();

    private CallToolResult caller(McpSyncServerExchange exchange, CallToolRequest request) {
        try {
            Object result = action(exchange, request.arguments());
            if (result instanceof CallToolResult cr) {
                return cr;
            }

            String text = objectMapper.writeValueAsString(result);
            var content = new McpSchema.TextContent(text);
            return new CallToolResult(List.of(content), false);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Object action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception;
}
