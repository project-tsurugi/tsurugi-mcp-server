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

import java.io.IOException;

import com.tsurugidb.iceaxe.transaction.TsurugiTransaction;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.mcp.server.dao.TsurugiMcpTransaction;

public class IceaxeTransaction implements TsurugiMcpTransaction {

    private final TsurugiTransaction transaction;

    public IceaxeTransaction(TsurugiTransaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void rollback() throws IOException, InterruptedException, TsurugiTransactionException {
        transaction.rollback();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        transaction.close();
    }
}
