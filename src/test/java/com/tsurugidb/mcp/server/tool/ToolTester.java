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
package com.tsurugidb.mcp.server.tool;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.util.JsonUtil;

public abstract class ToolTester {

    private static final String SYSPROP_DBTEST_ENDPOINT = "tsurugi.dbtest.endpoint";
    private static URI endpoint;

    protected static Arguments createTestArguments() {
        var arguments = new Arguments();

        if (endpoint == null) {
            String s = System.getProperty(SYSPROP_DBTEST_ENDPOINT, "tcp://localhost:12345");
            endpoint = URI.create(s);

        }
        arguments.setConnectionUri(endpoint);

        return arguments;
    }

    protected static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper();
    }
}
