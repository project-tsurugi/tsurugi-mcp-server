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
import java.io.UncheckedIOException;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.iceaxe.util.InterruptedRuntimeException;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.mcp.server.dao.grpc.GrpcSessionPool;
import com.tsurugidb.mcp.server.dao.iceaxe.IceaxeSessionPool;

public abstract class SessionPool implements AutoCloseable {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public static SessionPool create(Arguments arguments) {
        var iceaxeEndpoint = arguments.getConnectionUri();
        if (iceaxeEndpoint != null) {
            return IceaxeSessionPool.create(arguments);
        }
        var grpcEndpoint = arguments.getGrpcEndpoint();
        if (grpcEndpoint != null) {
            return GrpcSessionPool.create(arguments);
        }

        throw new IllegalArgumentException("No connection endpoint specified.");
    }

    private final Deque<TsurugiMcpSession> sessionQueue = new ConcurrentLinkedDeque<>();
    private final Set<TsurugiMcpSession> sessionList = ConcurrentHashMap.newKeySet();
    private boolean closed = false;

    public final TsurugiMcpSession getSession() {
        var session = getSessionFromQueue();
        if (session == null) {
            synchronized (sessionList) {
                if (this.closed) {
                    throw new IllegalStateException("SessionPool is closed.");
                }

                try {
                    session = createSession();
                    sessionList.add(session);
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                } catch (InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                }
            }
        }
        return session;
    }

    private TsurugiMcpSession getSessionFromQueue() {
        for (;;) {
            var session = sessionQueue.pollFirst();
            if (session == null) {
                return null;
            }

            try {
                session.keepAlive();
                return session;
            } catch (Exception e) {
                LOG.trace("session keepAlive error", e);
            }

            try {
                session.actualClose();
            } catch (Exception e) {
                LOG.trace("session close error", e);
            }
            synchronized (sessionList) {
                if (!this.closed) {
                    sessionList.remove(session);
                }
            }
        }
    }

    protected abstract TsurugiMcpSession createSession() throws IOException, InterruptedException;

    void returnSession(TsurugiMcpSession session) {
        sessionQueue.push(session);
    }

    @Override
    public void close() {
        synchronized (sessionList) {
            if (this.closed) {
                return;
            }
            this.closed = true;
        }

        int count = 0, error = 0;
        for (var session : sessionList) {
            try {
                session.actualClose();
                count++;
            } catch (Exception e) {
                LOG.warn("session close error", e);
                error++;
            }
        }
        LOG.info("SessionPool closed. session={}, error={}", count, error);

        sessionList.clear();
    }
}
