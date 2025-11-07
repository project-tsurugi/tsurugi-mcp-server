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

import java.net.URI;

import com.tsurugidb.mcp.server.util.JsonUtil;

import io.modelcontextprotocol.json.McpJsonMapper;

public abstract class TsurugiMcpTester {

    private static final String SYSPROP_DBTEST_ENDPOINT = "tsurugi.dbtest.endpoint";
    private static final String SYSPROP_DBTEST_USER = "tsurugi.dbtest.user";
    private static final String SYSPROP_DBTEST_PASSWORD = "tsurugi.dbtest.password";
    private static final String SYSPROP_DBTEST_AUTH_TOKEN = "tsurugi.dbtest.auth-token";
    private static final String SYSPROP_DBTEST_CREDENTIALS = "tsurugi.dbtest.credentials";

    private static URI endpoint;

    protected static Arguments createTestArguments() {
        return createTestArguments(true);
    }

    protected static Arguments createTestArguments(boolean withCredential) {
        var arguments = new Arguments();

        if (endpoint == null) {
            String s = System.getProperty(SYSPROP_DBTEST_ENDPOINT, "tcp://localhost:12345");
            endpoint = URI.create(s);

        }
        arguments.setConnectionUri(endpoint);

        if (withCredential) {
            String user = findUser();
            if (user != null) {
                arguments.setUser(user);
                arguments.setPassword(findPassword());
            } else {
                String authToken = findAuthToken();
                if (authToken != null) {
                    arguments.setAuthToken(authToken);
                } else {
                    String credentials = findCredentials();
                    if (credentials != null) {
                        arguments.setCredentials(credentials);
                    }
                }
            }
        }

        return arguments;
    }

    public static String findUser() {
        return getSystemProperty(SYSPROP_DBTEST_USER);
    }

    public static String findPassword() {
        return getSystemProperty(SYSPROP_DBTEST_PASSWORD);
    }

    public static String findAuthToken() {
        return getSystemProperty(SYSPROP_DBTEST_AUTH_TOKEN);
    }

    public static String findCredentials() {
        return getSystemProperty(SYSPROP_DBTEST_CREDENTIALS);
    }

    private static String getSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value != null && value.isEmpty()) {
            return null;
        }
        return value;
    }

    protected static McpJsonMapper createJsonMapper() {
        return JsonUtil.createJsonMapper();
    }
}
