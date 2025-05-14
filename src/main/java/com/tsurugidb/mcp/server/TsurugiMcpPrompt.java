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

import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;

public class TsurugiMcpPrompt {

    private final Arguments arguments;

    public TsurugiMcpPrompt(Arguments arguments) {
        this.arguments = arguments;
    }

    public List<SyncPromptSpecification> syncPrompts() {
        if (!arguments.isPrompt()) {
            return List.of();
        }

        return List.of(tableListPrompt(), tableLMetadataPrompt(), queryPrompt());
    }

    private SyncPromptSpecification tableListPrompt() {
        var Prompt = new McpSchema.Prompt( //
                "tableList-prompt", // name //$NON-NLS-1$
                Messages.getString("TsurugiMcpPrompt.0"), // description //$NON-NLS-1$
                List.of() // arguments
        );
        return new SyncPromptSpecification(Prompt, this::tableList);
    }

    private GetPromptResult tableList(McpSyncServerExchange exchange, GetPromptRequest request) {
        String text = Messages.getString("TsurugiMcpPrompt.1"); //$NON-NLS-1$
        var content = new McpSchema.TextContent(text);
        var message = new PromptMessage(Role.USER, content);
        String description = Messages.getString("TsurugiMcpPrompt.2"); //$NON-NLS-1$
        return new GetPromptResult(description, List.of(message));
    }

    static final String TABLE_NAME = "tableName"; //$NON-NLS-1$

    private SyncPromptSpecification tableLMetadataPrompt() {
        var Prompt = new McpSchema.Prompt( //
                "tableMetadata-prompt", // name //$NON-NLS-1$
                Messages.getString("TsurugiMcpPrompt.3"), // description //$NON-NLS-1$
                List.of(new PromptArgument(TABLE_NAME, Messages.getString("TsurugiMcpPrompt.4"), true)) // arguments //$NON-NLS-1$
        );
        return new SyncPromptSpecification(Prompt, this::tableMetadata);
    }

    private GetPromptResult tableMetadata(McpSyncServerExchange exchange, GetPromptRequest request) {
        String tableName = (String) request.arguments().get(TABLE_NAME);

        String text = Messages.getString("TsurugiMcpPrompt.5").formatted(tableName); //$NON-NLS-1$
        var content = new McpSchema.TextContent(text);
        var message = new PromptMessage(Role.USER, content);
        String description = Messages.getString("TsurugiMcpPrompt.6").formatted(tableName); //$NON-NLS-1$
        return new GetPromptResult(description, List.of(message));
    }

    private SyncPromptSpecification queryPrompt() {
        var Prompt = new McpSchema.Prompt( //
                "query-prompt", // name //$NON-NLS-1$
                Messages.getString("TsurugiMcpPrompt.7"), // description //$NON-NLS-1$
                List.of(new PromptArgument(TABLE_NAME, Messages.getString("TsurugiMcpPrompt.8"), true)) // arguments //$NON-NLS-1$
        );
        return new SyncPromptSpecification(Prompt, this::query);
    }

    private GetPromptResult query(McpSyncServerExchange exchange, GetPromptRequest request) {
        String tableName = (String) request.arguments().get(TABLE_NAME);

        String text = Messages.getString("TsurugiMcpPrompt.9").formatted(tableName); //$NON-NLS-1$
        var content = new McpSchema.TextContent(text);
        var message = new PromptMessage(Role.USER, content);
        String description = Messages.getString("TsurugiMcpPrompt.10").formatted(tableName); //$NON-NLS-1$
        return new GetPromptResult(description, List.of(message));
    }
}
