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
package com.tsurugidb.mcp.server.dao.grpc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.grpc.client.common.CommonOption;
import com.tsurugidb.grpc.client.connection.Connection;
import com.tsurugidb.grpc.client.connection.ConnectionOption;
import com.tsurugidb.grpc.client.exception.ServerException;
import com.tsurugidb.grpc.client.exception.code.CoreDiagnosticCode;
import com.tsurugidb.grpc.client.session.Credential;
import com.tsurugidb.grpc.client.session.Session;
import com.tsurugidb.grpc.client.session.SessionOption;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.dao.SessionPool;
import com.tsurugidb.mcp.server.dao.TsurugiMcpSession;

public class GrpcSessionPool extends SessionPool {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcSessionPool.class);

    public static GrpcSessionPool create(Arguments arguments) {
        CommonOption commonOption;
        {
            var builder = CommonOption.newBuilder();
            arguments.findDbTimeout().ifPresent(timeout -> builder.timeout(Duration.ofSeconds(timeout)));
            commonOption = builder.build();
        }

        String endpoint = arguments.getGrpcEndpoint();
        boolean secure = arguments.isGrpcSecure();
        var connectionOption = ConnectionOption.newBuilder() //
                .target(endpoint) //
                .secure(secure) //
                .defaultCommonOption(commonOption) //
                .build();
        var connection = Connection.create(connectionOption);

        var credentialList = GrpcCredentialUtil.getCredential(arguments);
        SessionOption sessionOption;
        {
            var builder = SessionOption.newBuilder();
            builder.applicationName("Tsurugi MCP server");
            builder.sessionLabel(arguments.getConnectionLabel());
            sessionOption = builder.build();
        }

        var pool = getConnection(connection, credentialList, sessionOption);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.debug("shutdownHook start");
                pool.close();
                LOG.debug("shutdownHook end");
            }
        });
        return pool;
    }

    private static GrpcSessionPool getConnection(Connection connection, List<Credential> credentialList, SessionOption sessionOption) {
        var attemptFailures = new ArrayList<Exception>();
        for (var credential : credentialList) {
            var option = SessionOption.newBuilder(sessionOption).credential(credential).build();
            try (var session = Session.create(connection, option)) {
                return new GrpcSessionPool(connection, option);
            } catch (ServerException e) {
                var code = e.getDiagnosticCode();
                if (code == CoreDiagnosticCode.AUTHENTICATION_ERROR || code == CoreDiagnosticCode.INVALID_REQUEST) {
                    LOG.debug("authentication error in connection attempt. {}: {}", credential, e.getMessage());
                    attemptFailures.add(e);
                    continue;
                }
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (attemptFailures.isEmpty()) {
            throw new RuntimeException("connect error");
        }

        var last = attemptFailures.getLast();
        var e = new RuntimeException("connect error", last);
        for (var s : attemptFailures.subList(0, attemptFailures.size() - 1)) {
            e.addSuppressed(s);
        }
        throw e;
    }

    private final Connection connection;
    private final SessionOption sessionOption;

    private GrpcSessionPool(Connection connection, SessionOption option) {
        this.connection = connection;
        this.sessionOption = option;
    }

    @Override
    protected TsurugiMcpSession createSession() throws IOException, InterruptedException, ServerException {
        var session = Session.create(connection, sessionOption);
        return new GrpcSession(this, session);
    }
}
