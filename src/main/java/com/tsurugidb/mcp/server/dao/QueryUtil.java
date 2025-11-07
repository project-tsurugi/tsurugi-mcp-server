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
package com.tsurugidb.mcp.server.dao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.iceaxe.exception.TsurugiExceptionUtil;
import com.tsurugidb.iceaxe.session.TsurugiSession;
import com.tsurugidb.iceaxe.sql.TsurugiSqlQuery;
import com.tsurugidb.iceaxe.sql.result.TgResultMapping;
import com.tsurugidb.iceaxe.sql.result.TsurugiQueryResult;
import com.tsurugidb.iceaxe.sql.result.TsurugiResultRecord;
import com.tsurugidb.iceaxe.sql.type.TgBlobReference;
import com.tsurugidb.iceaxe.sql.type.TgClobReference;
import com.tsurugidb.iceaxe.transaction.TgCommitType;
import com.tsurugidb.iceaxe.transaction.TsurugiTransaction;
import com.tsurugidb.iceaxe.transaction.exception.TsurugiTransactionException;
import com.tsurugidb.iceaxe.transaction.option.TgTxOption;
import com.tsurugidb.iceaxe.util.InterruptedRuntimeException;
import com.tsurugidb.mcp.server.Arguments;

import io.modelcontextprotocol.json.McpJsonMapper;

public class QueryUtil {
    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

    private static final AtomicInteger QUERY_ID = new AtomicInteger();

    private final McpJsonMapper jsonMapper;
    private final SessionPool pool;
    private final int limitSize;
    private final Map<String, QueryCache> queryMap = new ConcurrentHashMap<>();

    public QueryUtil(McpJsonMapper jsonMapper, Arguments arguments, SessionPool pool) {
        this.jsonMapper = jsonMapper;
        this.pool = pool;
        this.limitSize = arguments.getResponseLimitSize();
    }

    public QueryResult execute(String sql, TgTxOption txOption, String cursor) {
        QueryCache cache;
        if (cursor == null) {
            cache = new QueryCache(pool.getSession());
            cache.initialize(sql, txOption);
        } else {
            cache = queryMap.remove(cursor);
            if (cache == null) {
                throw new RuntimeException(MessageFormat.format("not found ongoing query. cursor={0}", cursor));
            }
        }

        var result = cache.execute();

        String nextCursor = result.nextCursor();
        if (nextCursor != null) {
            queryMap.put(nextCursor, cache);
        }

        return result;
    }

    public record QueryResult(List<Map<String, Object>> rows, boolean hasMoreRecord, String nextCursor, String serializationFailureMessage) {
    }

    private class QueryCache implements AutoCloseable {
        private final int queryId;
        private TsurugiSession session;
        private TsurugiSqlQuery<TsurugiResultRecord> ps;
        private TsurugiTransaction transaction;
        private TsurugiQueryResult<TsurugiResultRecord> queryResult;
        private List<Map<String, Object>> prevList = new ArrayList<>();
        private int prevSize;
        private boolean finish = false;

        QueryCache(TsurugiSession session) {
            this.queryId = QUERY_ID.getAndIncrement();
            this.session = session;
        }

        public void initialize(String sql, TgTxOption txOption) {
            try (var t = this) {
                try {
                    var resultMapping = TgResultMapping.of(record -> record);
                    this.ps = session.createQuery(sql, resultMapping);
                    this.transaction = session.createTransaction(txOption);
                    this.queryResult = transaction.executeQuery(ps);
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                } catch (InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                } catch (TsurugiTransactionException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                LOG.warn("QueryCache.initialize error", e);
                this.finish = true;
                throw e;
            }
        }

        public QueryResult execute() {
            try (var t = this) {
                var list = new ArrayList<Map<String, Object>>();
                list.addAll(this.prevList);
                prevList.clear();
                int estimateTotalSize = 128 + this.prevSize;
                prevSize = 0;

                String serializationFauluerMessage = null;
                try {
                    boolean doCommit = false;
                    for (;;) {
                        var recordOpt = queryResult.findRecord();
                        if (recordOpt.isEmpty()) {
                            doCommit = true;
                            this.finish = true;
                            break;
                        }
                        var record = recordOpt.get();

                        var nameList = record.getNameList();
                        int size = nameList.size();
                        var map = new LinkedHashMap<String, Object>(size);
                        for (int j = 0; j < size; j++) {
                            String name = nameList.get(j);
                            Object value = convert(record.getValueOrNull(j));
                            map.put(name, value);
                        }
                        String text = jsonMapper.writeValueAsString(map);
//                      int estimateSize = text.length() + 8;
                        int estimateSize = text.getBytes(StandardCharsets.UTF_8).length + 8;
                        if (estimateTotalSize + estimateSize >= limitSize) {
                            prevList.add(map);
                            this.prevSize = estimateSize;
                            break;
                        }
                        estimateTotalSize += estimateSize;
                        list.add(map);
                    }

                    if (doCommit) {
                        transaction.commit(TgCommitType.DEFAULT);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                } catch (InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                } catch (TsurugiTransactionException e) {
                    if (TsurugiExceptionUtil.getInstance().isSerializationFailure(e)) {
                        serializationFauluerMessage = e.getMessage();
                        this.finish = true;
                    } else {
                        throw new RuntimeException(e);
                    }
                }

                boolean hasMore = !this.finish;
                String nextCursor = this.finish ? null : "query" + queryId;
                return new QueryResult(list, hasMore, nextCursor, serializationFauluerMessage);
            } catch (Exception e) {
                LOG.warn("QueryCache.execute error", e);
                this.finish = true;
                throw e;
            }
        }

        @Override
        public void close() {
            if (finish) {
                try (var s = session; var p = ps; var t = transaction; var qr = queryResult) {
                    // close only
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                } catch (InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                } catch (TsurugiTransactionException e) {
                    throw new RuntimeException(e);
                } finally {
                    this.session = null;
                    this.ps = null;
                    this.transaction = null;
                    this.queryResult = null;
                }
            }
        }
    }

    Object convert(Object value) throws IOException, InterruptedException, TsurugiTransactionException {
        if (value instanceof TgBlobReference blob) {
            return blob.readAllBytes();
        }
        if (value instanceof TgClobReference clob) {
            return clob.readString();
        }

        return value;
    }
}
