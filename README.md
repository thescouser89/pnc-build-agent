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
