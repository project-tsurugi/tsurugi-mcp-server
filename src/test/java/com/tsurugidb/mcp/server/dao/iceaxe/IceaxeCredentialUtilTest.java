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
package com.tsurugidb.mcp.server.dao.iceaxe;

import static org.junit.Assume.assumeNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.grpc.client.connection.Connection;
import com.tsurugidb.grpc.client.connection.ConnectionOption;
import com.tsurugidb.grpc.client.exception.TsurugiGrpcException;
import com.tsurugidb.grpc.client.session.Session;
import com.tsurugidb.grpc.client.session.SessionOption;
import com.tsurugidb.iceaxe.TsurugiConnector;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.TsurugiMcpTester;
import com.tsurugidb.mcp.server.dao.grpc.GrpcCredentialUtil;

class IceaxeCredentialUtilTest extends TsurugiMcpTester {

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void userPassword(TsurugiMode mode) throws Exception {
        String user = findUser();
        assumeNotNull(user);

        var arguments = createTestArguments(mode, false);
        arguments.setUser(user);
        arguments.setPassword(findPassword());

        connectTest(mode, arguments);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void authToken(TsurugiMode mode) throws Exception {
        String authToken = findAuthToken();
        assumeNotNull(authToken);

        var arguments = createTestArguments(mode, false);
        arguments.setAuthToken(authToken);

        connectTest(mode, arguments);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ICEAXE", "GRPC" })
    void fileCredential(TsurugiMode mode) throws Exception {
        String credentials = findCredentials();
        assumeNotNull(credentials);

        var arguments = createTestArguments(mode, false);
        arguments.setCredentials(credentials);

        connectTest(mode, arguments);
    }

    private void connectTest(TsurugiMode mode, Arguments arguments) throws IOException, InterruptedException {
        switch (mode) {
        case ICEAXE:
            iceaxeConnectTest(arguments);
            break;
        case GRPC:
            gprcConnectTest(arguments);
            break;
        }
    }

    private void iceaxeConnectTest(Arguments arguments) throws IOException, InterruptedException {
        URI endpoint = arguments.getConnectionUri();
        var credentialList = IceaxeCredentialUtil.getCredential(arguments);
        assertEquals(1, credentialList.size());
        var credential = credentialList.getFirst();
        var connector = TsurugiConnector.of(endpoint, credential);

        try (var session = connector.createSession()) {
            session.getLowSession();
        }
    }

    private void gprcConnectTest(Arguments arguments) throws TsurugiGrpcException, InterruptedException {
        String endpoint = arguments.getGrpcEndpoint();
        var credentialList = GrpcCredentialUtil.getCredential(arguments);
        assertEquals(1, credentialList.size());
        var credential = credentialList.getFirst();
        var connectionOption = ConnectionOption.newBuilder() //
                .target(endpoint) //
//              .secure(secure) //
//              .defaultCommonOption(commonOption) //
                .build();
        var connection = Connection.create(connectionOption);

        var sessionOption = SessionOption.newBuilder() //
                .applicationName("Tsurugi MCP server") //
                .credential(credential) //
                .build();
        try (var session = Session.create(connection, sessionOption)) {
        }
    }
}
