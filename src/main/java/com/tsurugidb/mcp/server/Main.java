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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.tsurugidb.mcp.server.util.JsonUtil;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var arguments = new Arguments();
        var analyzer = JCommander.newBuilder().programName(TsurugiMcpServer.SERVER_NAME).addObject(arguments).build();
        analyzer.parse(args);

        if (arguments.isPrintHelp()) {
            analyzer.usage();
            System.out.println("tools:");
            for (String name : TsurugiMcpTool.toolNames()) {
                System.out.println("  " + name);
            }
            return;
        }

        new Main().main(arguments);
    }

    public void main(Arguments arguments) {
        var objectMapper = JsonUtil.createObjectMapper();

        McpServerTransportProvider transport = new StdioServerTransportProvider(objectMapper);
        McpSyncServer server = TsurugiMcpServer.syncServer(transport, objectMapper, arguments);
        LOG.info("serverInfo={}", server.getServerInfo());
    }
}
