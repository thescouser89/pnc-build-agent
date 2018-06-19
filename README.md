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
