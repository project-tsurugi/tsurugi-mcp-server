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

import com.tsurugidb.mcp.server.dao.SessionPool;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import reactor.core.publisher.Mono;

public class TsurugiMcpServerTransportProvider extends StdioServerTransportProvider {

    private final SessionPool pool;

    public TsurugiMcpServerTransportProvider(McpJsonMapper jsonMapper, SessionPool pool) {
        super(jsonMapper);
        this.pool = pool;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        var factory = new McpServerSession.Factory() {
            @Override
            public McpServerSession create(McpServerTransport sessionTransport) {
                var transport = new McpServerTransport() {

                    @Override
                    public Mono<Void> sendMessage(JSONRPCMessage message) {
                        return sessionTransport.sendMessage(message);
                    }

                    @Override
                    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
                        return sessionTransport.unmarshalFrom(data, typeRef);
                    }

                    @Override
                    public Mono<Void> closeGracefully() {
                        pool.close();
                        return sessionTransport.closeGracefully();
                    }

                    @Override
                    public void close() {
                        pool.close();
                        sessionTransport.close();
                    }
                };
                return sessionFactory.create(transport);
            }
        };
        super.setSessionFactory(factory);
    }
}
