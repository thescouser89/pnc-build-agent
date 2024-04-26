package org.jboss.pnc.buildagent.server;

public class KeycloakOfflineOIDCFilterConfiguration {

    /**
     * The public key of the realm (realm-public-key), as specified in the {auth url}/auth/realms/{realm}
     */
    private String publicKey;


    /**
     * The auth server url: should be in format {auth url}/auth/realms/{realm} (similar to how Quarkus auth wants it)
     */
    private String authServerUrl;

    public KeycloakOfflineOIDCFilterConfiguration(String publicKey, String authServerUrl) {
        this.publicKey = publicKey;
        this.authServerUrl = authServerUrl;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }
}
