## Build stage ##
FROM maven:3.9.9-eclipse-temurin-21 as deps

WORKDIR /app

COPY pom.xml /app

RUN mvn dependency:go-offline

FROM maven:3.9.9-eclipse-temurin-21 as build

WORKDIR /app

COPY . /app
COPY --from=deps /root/.m2 /root/.m2

RUN mvn clean package -Dmaven.test.skip=true

## Run stage ##
FROM openjdk:21 as run

WORKDIR /app

RUN mkdir -p /app/uploads && mkdir -p /app/config && mkdir -p /app/log

COPY --from=build /app/target/*.jar /app/cake_ai.jar

ENTRYPOINT ["java","-jar","/app/cake_ai.jar","--spring.config.name=application","--spring.config.location=/app/config/application.properties"]
