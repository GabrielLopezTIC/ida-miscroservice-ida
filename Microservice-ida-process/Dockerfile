FROM openjdk:8
VOLUME \tmp
EXPOSE 2222
ADD ./target/Microservice-ida-process-0.0.1-SNAPSHOT.jar Microservice-Ida.jar
ENTRYPOINT ["java","-jar","/Microservice-Ida.jar"]