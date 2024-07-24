FROM openjdk:15-jdk-slim
EXPOSE 8080
ARG JAR_FILE_TRACIN=target/tracing-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE_TRACIN} Tracing.jar
