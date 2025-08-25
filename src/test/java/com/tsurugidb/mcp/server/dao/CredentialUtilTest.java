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
package com.tsurugidb.mcp.server.dao;

import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import com.tsurugidb.iceaxe.TsurugiConnector;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.TsurugiMcpTester;

class CredentialUtilTest extends TsurugiMcpTester {

    @Test
    void userPassword() throws Exception {
        String user = findUser();
        assumeNotNull(user);

        var arguments = createTestArguments(false);
        arguments.setUser(user);
        arguments.setPassword(findPassword());

        connectTest(arguments);
    }

    @Test
    void authToken() throws Exception {
        String authToken = findAuthToken();
        assumeNotNull(authToken);

        var arguments = createTestArguments(false);
        arguments.setAuthToken(authToken);

        connectTest(arguments);
    }

    @Test
    void fileCredential() throws Exception {
        String credentials = findCredentials();
        assumeNotNull(credentials);

        var arguments = createTestArguments(false);
        arguments.setCredentials(credentials);

        connectTest(arguments);
    }

    private void connectTest(Arguments arguments) throws IOException, InterruptedException {
        URI endpoint = arguments.getConnectionUri();
        var credential = CredentialUtil.getCredential(arguments);
        var connector = TsurugiConnector.of(endpoint, credential);

        try (var session = connector.createSession()) {
            session.getLowSession();
        }
    }
}
