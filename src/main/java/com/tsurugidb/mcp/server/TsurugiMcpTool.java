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

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.tool.AbstractTool;
import com.tsurugidb.mcp.server.tool.DdlTool;
import com.tsurugidb.mcp.server.tool.TableMetadataTool;
import com.tsurugidb.mcp.server.tool.TableNamesTool;
import com.tsurugidb.mcp.server.tool.QueryTool;
import com.tsurugidb.mcp.server.tool.UpdateTool;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

public class TsurugiMcpTool {
    private static final Logger LOG = LoggerFactory.getLogger(TsurugiMcpTool.class);

    private static final List<AbstractTool> TOOLS = List.of( //
            new TableNamesTool(), //
            new TableMetadataTool(), //
            new QueryTool(), //
            new UpdateTool(), //
            new DdlTool() //
    );

    public static List<String> toolNames() {
        return TOOLS.stream().map(AbstractTool::toolName).toList();
    }

    public static List<SyncToolSpecification> syncTools(ObjectMapper objectMapper, Arguments arguments, SessionPool pool) {
        var set = new HashSet<>(arguments.getEnableToolList());
        for (String name : arguments.getDisableToolList()) {
            set.remove(name);
        }
        LOG.info("enableToolSet={}", set);

        return TOOLS.stream() //
                .filter(t -> set.contains(t.toolName())) //
                .peek(t -> t.initialize(objectMapper, arguments, pool)) //
                .map(AbstractTool::syncTool) //
                .toList();
    }
}
