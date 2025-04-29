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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.dao.QueryUtil;
import com.tsurugidb.mcp.server.dao.QueryUtil.QueryResult;
import com.tsurugidb.mcp.server.dao.SessionPool;

import io.modelcontextprotocol.server.McpSyncServerExchange;

public class QueryTool extends AbstractTool {

    private QueryUtil queryUtil;

    @Override
    public void initialize(ObjectMapper objectMapper, Arguments arguments, SessionPool pool) {
        super.initialize(objectMapper, arguments, pool);

        this.queryUtil = new QueryUtil(objectMapper, arguments, pool);
    }

    @Override
    public String toolName() {
        return "query";
    }

    @Override
    protected String toolDescription() {
        return "execute SQL (select only) in Tsurugi RDBMS";
    }

    static final String SQL = "sql";
    static final String TRANSACTION_TYPE = "transaction_type";
    static final String CURSOR = "cursor";

    @Override
    protected List<ToolProperty> properties() {
        return List.of( //
                ToolProperty.of(SQL, "SQL (select only) to execute", true), //
                ToolProperty.of(TRANSACTION_TYPE, "transaction type. `OCC`, `LTX` or `RTX`. default is `RTX`", false), //
                ToolProperty.of(CURSOR, "optional cursor value", false) //
        );
    }

    @Override
    protected QueryResult action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception {
        String sql = (String) arguments.get(SQL);
        var txOption = getTransactionOption(arguments);
        String cursor = (String) arguments.get(CURSOR);

        return queryUtil.execute(sql, txOption, cursor);
    }

    TgTxOption getTransactionOption(Map<String, Object> arguments) {
        String transactionType = (String) arguments.get(TRANSACTION_TYPE);
        if (transactionType == null) {
            return TgTxOption.ofRTX();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> TgTxOption.ofLTX();
        case "RTX", "READ ONLY" -> TgTxOption.ofRTX();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }
}
