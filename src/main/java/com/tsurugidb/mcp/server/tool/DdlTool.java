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

import com.tsurugidb.iceaxe.transaction.TgCommitType;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;

import io.modelcontextprotocol.server.McpSyncServerExchange;

public class DdlTool extends AbstractTool {

    @Override
    public String toolName() {
        return "executeDdl";
    }

    @Override
    protected String toolDescription() {
        return "execute DDL (create, drop) in Tsurugi RDBMS";
    }

    static final String SQL = "sql";
    static final String TRANSACTION_TYPE = "transaction_type";

    @Override
    protected List<ToolProperty> properties() {
        return List.of( //
                ToolProperty.of(SQL, "SQL (insert, update, delete) to execute", true), //
                ToolProperty.of(TRANSACTION_TYPE, "transaction type. `OCC`, `LTX`. default is `OCC`", false) //
        );
    }

    @Override
    protected String action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception {
        String sql = (String) arguments.get(SQL);
        var txOption = getTransactionOption(arguments);

        try (var session = pool.getSession(); //
                var transaction = session.createTransaction(txOption); //
                var ps = session.createStatement(sql)) {
            transaction.executeAndGetCountDetail(ps);

            transaction.commit(TgCommitType.DEFAULT);
        }

        return "succeeded";
    }

    TgTxOption getTransactionOption(Map<String, Object> arguments) {
        String transactionType = (String) arguments.get(TRANSACTION_TYPE);
        if (transactionType == null) {
            return TgTxOption.ofOCC();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> TgTxOption.ofDDL();
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }
}
