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
package com.tsurugidb.mcp.server.dao.grpc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.grpc.client.session.Credential;
import com.tsurugidb.mcp.server.Arguments;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;

public class GrpcCredentialUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcCredentialUtil.class);

    public static List<Credential> getCredential(Arguments arguments) {
        var list = getCredentialList(arguments);
        switch (list.size()) {
        case 0:
            return getDefaultCredential();
        case 1:
            return list;
        default:
            throw new RuntimeException("specify only one of [--user, --auth-token, --credentials, --no-auth]");
        }
    }

    private static List<Credential> getCredentialList(Arguments arguments) {
        var list = new ArrayList<Credential>();

        String user = arguments.getUser();
        LOG.debug("--user={}", user);
        if (user != null) {
            String password = arguments.getPassword();
            list.add(Credential.of(user, password));
        }

        String authToken = arguments.getAuthToken();
        LOG.debug("--auth-token={}", debugAuthToken(authToken));
        if (authToken != null) {
            list.add(Credential.ofAuthToken(authToken));
        }

        String credentials = arguments.getCredentials();
        LOG.debug("--credentials={}", credentials);
        if (credentials != null) {
            var path = Path.of(credentials);
            try {
                list.add(Credential.load(path));
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }

        Boolean noAuth = arguments.getNoAuth();
        LOG.debug("--no-auth={}", noAuth);
        if ((noAuth != null) && noAuth) {
            list.add(Credential.empty());
        }

        return list;
    }

    private static List<Credential> getDefaultCredential() {
        var result = new ArrayList<Credential>();

        var tokenOpt = Optional.ofNullable(System.getenv("TSURUGI_AUTH_TOKEN")) //
                .filter(token -> !token.isEmpty());
        if (tokenOpt.isPresent()) {
            String authToken = tokenOpt.get();
            LOG.debug("TSURUGI_AUTH_TOKEN={}", debugAuthToken(authToken));
            result.add(Credential.ofAuthToken(authToken));
        }

        var pathOpt = FileCredential.DEFAULT_CREDENTIAL_PATH.filter(path -> Files.exists(path));
        if (pathOpt.isPresent()) {
            Path path = pathOpt.get();
            LOG.debug("default.credentials={}", path);
            try {
                result.add(Credential.load(path));
            } catch (IOException e) {
                LOG.warn("error occurred while loading the default credential file", e);
            }
        }

        result.add(Credential.empty());

        return result;
    }

    private static String debugAuthToken(String authToken) {
        if (authToken != null) {
            return authToken.substring(0, Math.min(authToken.length(), 16)) + "****";
        } else {
            return null;
        }
    }
}
