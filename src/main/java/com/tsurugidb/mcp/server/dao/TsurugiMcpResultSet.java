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
package com.tsurugidb.mcp.server.dao;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;

public interface TsurugiMcpResultSet extends AutoCloseable {

    public @Nullable Map<String, Object> nextRow() throws IOException, InterruptedException, TsurugiTransactionException;

    public void commit() throws IOException, InterruptedException, TsurugiTransactionException;

    @Override
    public void close() throws IOException, InterruptedException, TsurugiTransactionException;
}
