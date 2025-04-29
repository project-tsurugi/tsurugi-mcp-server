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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tsurugidb.iceaxe.transaction.TgCommitType;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;

import io.modelcontextprotocol.server.McpSyncServerExchange;

public class UpdateTool extends AbstractTool {

    @Override
    public String toolName() {
        return "update";
    }

    @Override
    protected String toolDescription() {
        return "execute SQL (insert, update, delete) in Tsurugi RDBMS";
    }

    static final String SQL = "sql";
    static final String TRANSACTION_TYPE = "transaction_type";
    static final String WRITE_PRESERVE = "write_preserve";

    @Override
    protected List<ToolProperty> properties() {
        return List.of( //
                ToolProperty.of(SQL, "SQL (insert, update, delete) to execute", true), //
                ToolProperty.of(TRANSACTION_TYPE, "transaction type. `OCC`, `LTX`. default is `OCC`", false), //
                ToolProperty.of(WRITE_PRESERVE, "table names for target (comma separate). Required when transaction_type is LTX", false) //
        );
    }

    @Override
    protected Map<String, Long> action(McpSyncServerExchange exchange, Map<String, Object> arguments) throws Exception {
        String sql = (String) arguments.get(SQL);
        var txOption = getTransactionOption(arguments);

        var result = new LinkedHashMap<String, Long>();
        try (var session = pool.getSession(); //
                var transaction = session.createTransaction(txOption); //
                var ps = session.createStatement(sql)) {
            var count = transaction.executeAndGetCountDetail(ps);
            for (var entry : count.getLowCounterMap().entrySet()) {
                result.put(entry.getKey().name().toLowerCase(), entry.getValue());
            }

            transaction.commit(TgCommitType.DEFAULT);
        }

        return result;
    }

    TgTxOption getTransactionOption(Map<String, Object> arguments) {
        String transactionType = (String) arguments.get(TRANSACTION_TYPE);
        if (transactionType == null) {
            return TgTxOption.ofOCC();
        }

        return switch (transactionType.toUpperCase()) {
        case "OCC", "SHORT" -> TgTxOption.ofOCC();
        case "LTX", "LONG" -> {
            String wp = (String) arguments.get(WRITE_PRESERVE);
            if (wp == null) {
                yield TgTxOption.ofLTX();
            }
            var writePreserve = Arrays.stream(wp.split(",")).map(String::trim).toList();
            yield TgTxOption.ofLTX(writePreserve);
        }
        default -> throw new IllegalArgumentException("Unexpected transaction_type: " + transactionType);
        };
    }
}
