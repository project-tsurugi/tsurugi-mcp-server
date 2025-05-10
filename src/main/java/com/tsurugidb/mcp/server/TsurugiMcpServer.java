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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.mcp.server.dao.SessionPool;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

public class TsurugiMcpServer {

    public static final String SERVER_NAME = "tsurugi-mcp-server";
    public static final String SERVER_VERSION = "0.1.0";

    public static McpSyncServer syncServer(McpServerTransportProvider transportProvider, ObjectMapper objectMapper, Arguments arguments, SessionPool pool) {
        var tools = TsurugiMcpTool.syncTools(objectMapper, arguments, pool);
        var resources = new TsurugiMcpResource(objectMapper, arguments, pool).syncResources();

        var capabilities = ServerCapabilities.builder();
        if (!tools.isEmpty()) {
            capabilities.tools(false);
        }
        if (!resources.isEmpty()) {
            capabilities.resources(false, false);
        }
//      capabilities.logging();

        var server = McpServer.sync(transportProvider) //
                .serverInfo(SERVER_NAME, SERVER_VERSION) //
                .capabilities(capabilities.build()) //
                .tools(tools) //
                .resources(resources) //
                .build();
        return server;
    }
}
