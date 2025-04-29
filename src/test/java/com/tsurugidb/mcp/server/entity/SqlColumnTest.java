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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Empty;
import com.tsurugidb.iceaxe.metadata.TgSqlColumn;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.sql.proto.SqlCommon.AtomType;

class SqlColumnTest {

    @Test
    void getTypeDescriptionChar() {
        {
            var column = create(false, 10);
            assertEquals("10 byte UTF-8 text", SqlColumn.getTypeDescriptionChar(column));
        }
        {
            var column = create(false, 0);
            assertEquals("UTF-8 text", SqlColumn.getTypeDescriptionChar(column));
        }
        {
            var column = create(false, -1);
            assertEquals("UTF-8 text", SqlColumn.getTypeDescriptionChar(column));
        }
        {
            var column = create(true, 10);
            assertEquals("UTF-8 text of up to 10 bytes", SqlColumn.getTypeDescriptionChar(column));
        }
        {
            var column = create(true, 0);
            assertEquals("UTF-8 text", SqlColumn.getTypeDescriptionChar(column));
        }
        {
            var column = create(true, -1);
            assertEquals("UTF-8 text", SqlColumn.getTypeDescriptionChar(column));
        }
    }

    private static TgSqlColumn create(boolean varying, int length) {
        var builder = SqlCommon.Column.newBuilder().setName("foo").setAtomType(AtomType.CHARACTER);
        builder.setVarying(varying);
        if (length > 0) {
            builder.setLength(length);
        } else if (length < 0) {
            builder.setArbitraryLength(Empty.newBuilder().build());
        }
        return new TgSqlColumn(builder.build());
    }
}
