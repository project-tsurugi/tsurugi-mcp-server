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

import java.util.Optional;

import com.tsurugidb.iceaxe.metadata.ArbitraryInt;
import com.tsurugidb.iceaxe.metadata.TgSqlColumn;

public record SqlColumn(String columnName, String columnDescription, String columnType, String typeDescription, String constraint) {

    public static SqlColumn of(TgSqlColumn column) {
        String name = column.getName();
        String description = column.getDescription();
        String tType = column.getSqlTypeOrAtomTypeName();
        String typeDescription = getTypeDescription(column);
        String constraint = getConstraint(column);
        return new SqlColumn(name, description, tType, typeDescription, constraint);
    }

    static String getTypeDescription(TgSqlColumn column) {
        var atomType = column.getLowColumn().getAtomType();
        return switch (atomType) {
        case BOOLEAN -> "boolean";
        case INT4 -> "4 byte integer";
        case INT8 -> "8 byte integer";
        case FLOAT4 -> "4 byte floating point number";
        case FLOAT8 -> "8 byte floating point number";
        case DECIMAL -> "multi precision decimal number";
        case CHARACTER -> getTypeDescriptionChar(column);
        case OCTET -> "byte sequence";
        case BIT -> "bit sequence";
        case DATE -> "date (year, month, day)";
        case TIME_OF_DAY -> "time of day (hour, minute, second, nanosecond)";
        case TIME_POINT -> "time point (year, month, day, hour, minute, second, nanosecond)";
        case DATETIME_INTERVAL -> "date-time interval";
        case TIME_OF_DAY_WITH_TIME_ZONE -> "time of day with time zone (hour, minute, second, nanosecond, time-zone-offset)";
        case TIME_POINT_WITH_TIME_ZONE -> "time point with time zone (year, month, day, hour, minute, second, nanosecond, time-zone-offset)";
        case BLOB -> "binary large object (byte sequence)";
        case CLOB -> "character large object (text)";
        default -> "Tsurugi internal type name: " + atomType.name();
        };
    }

    static String getTypeDescriptionChar(TgSqlColumn column) {
        Optional<Boolean> varOpt = column.findVarying();
        if (varOpt.isPresent()) {
            if (varOpt.get()) { // VARCHAR
                Optional<ArbitraryInt> opt = column.findLength();
                if (opt.isPresent()) {
                    var length = opt.get();
                    if (!length.arbitrary()) {
                        return "UTF-8 text of up to " + length.value() + " bytes";
                    }
                }
            } else { // CHAR
                Optional<ArbitraryInt> opt = column.findLength();
                if (opt.isPresent()) {
                    var length = opt.get();
                    if (!length.arbitrary()) {
                        return length.value() + " byte UTF-8 text";
                    }
                }
            }
        }
        return "UTF-8 text";
    }

    static String getConstraint(TgSqlColumn column) {
        var sb = new StringBuilder();

        column.findNullable().ifPresent(b -> {
            sb.append(b ? "NULL" : "NOT NULL");
        });

        return sb.isEmpty() ? null : sb.toString();
    }
}
