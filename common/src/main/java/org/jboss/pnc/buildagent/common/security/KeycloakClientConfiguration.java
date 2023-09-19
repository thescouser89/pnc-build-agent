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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * DTO for Keycloak Client Configuration; it's a companion class for KeycloakClient
 */
public class KeycloakClientConfiguration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    private String url;
    private String realm;
    private String clientId;
    private String clientSecret;

    /**
     * Parse a JSON file containing the appropriate configuration to produce a KeycloakClientConfiguration
     *
     * @param file to parse
     * @return KeycloakClientConfiguration
     * @throws KeycloakClientConfigurationException if parsing went wrong
     */
    public static KeycloakClientConfiguration parseJson(File file) throws KeycloakClientConfigurationException {
        try {
            return OBJECT_MAPPER.readValue(file, KeycloakClientConfiguration.class);
        } catch (Exception e) {
            throw new KeycloakClientConfigurationException(e.getMessage());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
