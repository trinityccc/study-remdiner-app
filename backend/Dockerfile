FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV PORT=8080
EXPOSE 8080

COPY --from=build /app/target/*.jar /app/app.jar
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
