FROM openjdk:25-ea-jdk-slim
WORKDIR /app
COPY target/jini-example-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $0 $@"]
