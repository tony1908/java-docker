FROM maven:3.8.4-openjdk-11-slim AS builder

WORKDIR /app

COPY src/ ./src/
COPY pom.xml ./

RUN mvn clean package

FROM openjdk:11-jre-slim AS runner

WORKDIR /app

COPY --from=builder /app/target/javadb-1.0-SNAPSHOT.jar /app/javadb-1.0-SNAPSHOT.jar

CMD ["java", "-jar", "javadb-1.0-SNAPSHOT.jar"]

