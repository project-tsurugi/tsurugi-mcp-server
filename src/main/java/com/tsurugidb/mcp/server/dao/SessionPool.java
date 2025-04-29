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
import java.net.URI;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.iceaxe.TsurugiConnector;
import com.tsurugidb.iceaxe.session.TgSessionOption;
import com.tsurugidb.iceaxe.session.TgSessionOption.TgTimeoutKey;
import com.tsurugidb.iceaxe.session.TsurugiSession;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.util.FutureResponse;

public class SessionPool implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SessionPool.class);

    public static SessionPool create(Arguments arguments) {
        URI endpoint = arguments.getConnectionUri();
        var connector = TsurugiConnector.of(endpoint);
        LOG.debug("connector={}", connector);

        var sessionOption = TgSessionOption.of();
        {
            sessionOption.setLabel(arguments.getConnectionLabel());
            sessionOption.setTimeout(TgTimeoutKey.SESSION_CONNECT, arguments.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            sessionOption.setKeepAlive(false);
        }

        var pool = new SessionPool(connector, sessionOption);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                pool.close();
            }
        });
        return pool;
    }

    private final TsurugiConnector connector;
    private final TgSessionOption sessionOption;
    private final Deque<PoolSession> sessionQueue = new ConcurrentLinkedDeque<>();
    private final Set<PoolSession> sessionList = ConcurrentHashMap.newKeySet();

    private SessionPool(TsurugiConnector connector, TgSessionOption sessionOption) {
        this.connector = connector;
        this.sessionOption = sessionOption;
        connector.setSesionGenerator(PoolSession::new);
    }

    public TsurugiSession getSession() {
        var session = getSessionFromQueue();
        if (session == null) {
            try {
                session = (PoolSession) connector.createSession(sessionOption);
                sessionList.add(session);
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }
        return session;
    }

    private PoolSession getSessionFromQueue() {
        for (;;) {
            var session = sessionQueue.pollFirst();
            if (session != null) {
                if (!session.isAlive()) {
                    try {
                        session.actualClose();
                    } catch (Exception e) {
                        LOG.warn("session close error", e);
                    }
                    sessionList.remove(session);
                    continue;
                }
            }
            return session;
        }
    }

    private class PoolSession extends TsurugiSession {

        public PoolSession(FutureResponse<? extends Session> lowSessionFuture, TgSessionOption sessionOption) {
            super(lowSessionFuture, sessionOption);
        }

        @Override
        public void close() throws IOException, InterruptedException {
            sessionQueue.push(this);
        }

        void actualClose() throws IOException, InterruptedException {
            super.close();
        }
    }

    @Override
    public void close() {
        for (var session : sessionList) {
            try {
                session.actualClose();
            } catch (Exception e) {
                LOG.warn("session close error", e);
            }
        }
    }
}
