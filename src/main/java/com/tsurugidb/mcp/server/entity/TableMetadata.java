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
package com.tsurugidb.mcp.server.entity;

import java.util.List;

import com.tsurugidb.iceaxe.metadata.TgTableMetadata;

public record TableMetadata(String databaseName, String schemaName, String tableName, String tableDescription, List<SqlColumn> columns, List<String> primaryKeys) {

    public static TableMetadata of(TgTableMetadata metadata) {
        var columnList = metadata.getColumnList().stream().map(SqlColumn::of).toList();
        return new TableMetadata( //
                metadata.getDatabaseName(), //
                metadata.getSchemaName(), //
                metadata.getTableName(), //
                metadata.getDescription(), //
                columnList, //
                metadata.getPrimaryKeys());
    }
}
