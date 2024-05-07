package org.jboss.pnc.buildagent.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This DTO is used to map keycloak.json file to it so that we can do offline checking of tokens
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakOfflineOIDCFilterConfiguration {

    /**
     * The public key of the realm (realm-public-key), as specified in the {auth url}/auth/realms/{realm}
     */
    @JsonProperty("realm-public-key")
    private String realmPublicKey;

    /**
     * The auth server url: should be in format {auth url}/auth
     */
    @JsonProperty("auth-server-url")
    private String authServerUrl;

    @JsonProperty("realm")
    private String realm;

    // needed for jackson
    public KeycloakOfflineOIDCFilterConfiguration() {
    }

    public KeycloakOfflineOIDCFilterConfiguration(String publicKey, String authServerUrl, String realm) {
        this.realmPublicKey = publicKey;
        this.authServerUrl = authServerUrl;
        this.realm = realm;
    }

    public String getRealmPublicKey() {
        return realmPublicKey;
    }

    public void setRealmPublicKey(String publicKey) {
        this.realmPublicKey = publicKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
