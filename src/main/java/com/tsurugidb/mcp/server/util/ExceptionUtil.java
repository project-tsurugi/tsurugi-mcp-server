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
package com.tsurugidb.mcp.server.util;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class ExceptionUtil {

    public static CallToolResult createErrorToolResult(Throwable t) {
        String message = getMessage(t);
        String text = "failed: " + message;
        return CallToolResult.builder().addTextContent(text).isError(true).build();
    }

    private static String getMessage(Throwable t) {
        while (t != null) {
            String message = t.getMessage();
            if (message != null && !message.isEmpty()) {
                return message;
            }
            t = t.getCause();
        }
        return "unknown error";
    }
}
