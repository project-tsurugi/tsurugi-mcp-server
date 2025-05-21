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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Arguments {

    private URI connectionUri;
    private String connectionLabel = TsurugiMcpServer.SERVER_NAME;
    private long connectionTimeout = 30;
    private List<String> enableToolList = TsurugiMcpTool.toolNames();
    private List<String> disableToolList = new ArrayList<>();
    private boolean resourceEnable = true;
    private boolean promptEnable = true;
    private int responseLimistSize = 10 * 1024;
    private boolean printHelp;

    @Parameter(order = 10, //
            names = { "-c", "--connection" }, //
            arity = 1, //
            description = "Tsurugi server endpoint URI.", //
            required = true)
    public void setConnectionUri(URI uri) {
        Objects.requireNonNull(uri);
        this.connectionUri = uri;
    }

    public URI getConnectionUri() {
        return connectionUri;
    }

    @Parameter(order = 11, //
            names = { "--connection-label" }, //
            arity = 1, description = "Tsurugi connection session label.", //
            required = false)
    public void setConnectionLabel(String label) {
        this.connectionLabel = label;
    }

    public String getConnectionLabel() {
        return connectionLabel;
    }

    @Parameter(order = 12, //
            names = { "--connection-timeout" }, //
            arity = 1, //
            description = "Connection timeout (in seconds).", //
            required = false)
    public void setConnectionTimeout(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(MessageFormat.format("timeout must be >= 0 (specified: {0})", value));
        }
        this.connectionTimeout = value;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public static class ToolNameValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            var expected = new HashSet<>(TsurugiMcpTool.toolNames());
            for (String s : splitName(value)) {
                if (!expected.contains(s)) {
                    throw new ParameterException(MessageFormat.format("Parameter {0} is from {1}", s, expected));
                }
            }
        }
    }

    static List<String> splitName(String value) {
        String[] ss = value.split("[ \t\r\n,;\\[\\]]+");
        var list = new ArrayList<String>(ss.length);
        for (String s : ss) {
            String name = s.trim();
            if (!name.isEmpty()) {
                list.add(name);
            }
        }
        return list;
    }

    @Parameter(order = 20, //
            names = { "--enable-tools" }, //
            arity = 1, //
            description = "Enable tools (default is all tools)", //
            validateWith = ToolNameValidator.class, //
            required = false)
    public void setEnableToolList(String list) {
        this.enableToolList = splitName(list);
    }

    public List<String> getEnableToolList() {
        return this.enableToolList;
    }

    @Parameter(order = 21, //
            names = { "--disable-tools" }, //
            arity = 1, //
            description = "Disable tools", //
            validateWith = ToolNameValidator.class, //
            required = false)
    public void setDisableToolList(String list) {
        this.disableToolList = splitName(list);
    }

    public List<String> getDisableToolList() {
        return this.disableToolList;
    }

    @Parameter(order = 31, //
            names = { "--resource" }, //
            arity = 1, //
            description = "true: Enable resource", //
            required = false)
    public void setResource(boolean enable) {
        this.resourceEnable = enable;
    }

    public boolean isResource() {
        return this.resourceEnable;
    }

    @Parameter(order = 41, //
            names = { "--prompt" }, //
            arity = 1, //
            description = "true: Enable prompt", //
            required = false)
    public void setPrompt(boolean enable) {
        this.promptEnable = enable;
    }

    public boolean isPrompt() {
        return this.promptEnable;
    }

    @Parameter(order = 90, //
            names = { "--response-limit-size" }, //
            arity = 0, //
            description = "Limit size per page of query results [byte].", //
            required = false)
    public void setResponseLimitSize(int size) {
        this.responseLimistSize = size;
    }

    public int getResponseLimitSize() {
        return this.responseLimistSize;
    }

    @Parameter(order = 10000, //
            names = { "-h", "--help" }, //
            arity = 0, //
            description = "Print command help", //
            help = true)
    public void setPrintHelp(boolean enable) {
        this.printHelp = enable;
    }

    public boolean isPrintHelp() {
        return printHelp;
    }
}
