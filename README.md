Building and Packaging
======================


To compile project run

    mvn clean install

To pack all in one executable jar run

    mvn assembly:single

Running
=======
java -jar server/target/server-0.4-SNAPSHOT-jar-with-dependencies.jar -h

Example using Kafka destination: -kp enables Kafka, -pl defines is as the destination where the compelte log must be written.

    java -jar server/target/server-0.4-SNAPSHOT-jar-with-dependencies.jar -kp $PWD/kafka.properties -pl KAFKA

Authentication of Build Agent endpoints
=======================================
build-agent can be configured to require an OIDC token for some endpoints. This is done by either providing the `keycloakConfigurationFile` or the `keycloakOfflineConfigurationFile`.

The `keycloakConfigurationFile` follows the [keycloak.json](https://www.keycloak.org/docs/latest/securing_apps/) format and uses the discovery url ({auth url}/auth/realms/{realm}/.well-known/openid-configuration) to find the urls, public keys, and issuer information to validate the token.

In contrast, the `keycloakOfflineConfigurationFile` is used to validate the token using offline techniques only using the public key (obtained from {auth url}/auth/realms/{realm}) and the issuer url only. This is useful when we run build-agent inside a firewall that limits outgoing connections to other servers. The format of the file to provide is:
```json
{
  "publicKey": "public-key",
  "authServerUrl": "{auth-url}/auth/realms/{realm}"
}
```

If both options are specified, only the offline one will be used.

Keycloak Client
===============
The build-agent sends a callback on completion to the original caller. The callback needs to be authenticated to be
accepted by the original caller. To obtain the proper Authorization access token for the callback, use the CLI option:
```
-DkeycloakClientConfigFile=<filename>
```

The filename should have the following content:
```json
{
    "url": "https://keycloak-url",
    "realm": "realm",
    "clientId": "client-id",
    "clientSecret": "client-secret"
}
```

Only client credentials flow (service account authentication) is supported at this moment.
