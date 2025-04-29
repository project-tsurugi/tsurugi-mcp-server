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

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.iceaxe.session.TsurugiSession;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.entity.TableMetadata;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

public class TsurugiMcpResource {

    private final ObjectMapper objectMapper;
    private final Arguments arguments;
    private final SessionPool pool;

    public TsurugiMcpResource(ObjectMapper objectMapper, Arguments arguments, SessionPool pool) {
        this.objectMapper = objectMapper;
        this.arguments = arguments;
        this.pool = pool;
    }

    protected TsurugiSession getSession() throws IOException {
        return pool.getSession();
    }

    public List<SyncResourceSpecification> syncResources() {
        if (!arguments.isResource()) {
            return List.of();
        }

        try (var session = getSession()) {
            List<String> tableNames = session.getTableNameList();

            var list = new ArrayList<SyncResourceSpecification>(tableNames.size());
            for (String tableName : tableNames) {
                list.add(tableSchemaResource(tableName));
            }
            return list;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SyncResourceSpecification tableSchemaResource(String tableName) {
        var resource = new McpSchema.Resource( //
                "tsurugidb://%s/schema".formatted(tableName), // uri
                "%s table schema".formatted(tableName), // name
                "'%s' table schema in Tsurugi database".formatted(tableName), // description
                "application/json", // mimeType
                null // annotations
        );
        return new SyncResourceSpecification(resource, this::tableSchema);
    }

    ReadResourceResult tableSchema(McpSyncServerExchange exchange, ReadResourceRequest request) {
        var uri = URI.create(request.uri());
        String tableName = uri.getHost();

        String text;
        try (var session = getSession()) {
            var opt = session.findTableMetadata(tableName);
            if (opt.isEmpty()) {
                throw new RuntimeException(MessageFormat.format("table not found. tableName={0}", tableName));
            }
            var metadata = TableMetadata.of(opt.get());

            text = objectMapper.writeValueAsString(metadata);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var content = new TextResourceContents(request.uri(), "application/json", text);
        return new ReadResourceResult(List.of(content));
    }
}
