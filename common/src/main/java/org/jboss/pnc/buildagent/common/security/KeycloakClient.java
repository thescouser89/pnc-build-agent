/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.buildagent.common.security;

import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * KeycloakClient to get an access token from the OIDC server to send to authenticated endpoints.
 * It only supports service account clientId / clientSecret (client credentials flow) for now but can easily be changed
 * in the future to support other styles of authentication.
 */
public class KeycloakClient {

    private final String url;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);

    public KeycloakClient(String url, String realm, String clientId, String clientSecret) {
        this.url = url;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public KeycloakClient(KeycloakClientConfiguration configuration) {
        this.url = configuration.getUrl();
        this.realm = configuration.getRealm();
        this.clientId = configuration.getClientId();
        this.clientSecret = configuration.getClientSecret();
    }

    /**
     * Get a fresh access token from the OIDC server
     * @return access token
     */
    public String getAccessToken() {
        final Configuration configuration = new Configuration(
                url,
                realm,
                clientId,
                Collections.singletonMap("secret", clientSecret),
                HttpClients.createDefault());

        logger.info("Getting access token!");
        String accessToken =  AuthzClient.create(configuration).obtainAccessToken().getToken();
        logger.info("Received access token! :: {}", accessToken);
        return accessToken;
    }
}
