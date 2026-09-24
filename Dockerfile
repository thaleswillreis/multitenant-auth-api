# ---- Estagio 1: build ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Estagio 2: runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S authapi && adduser -S authapi -G authapi

WORKDIR /app
COPY --from=build /app/target/multitenant-auth-api-*.jar app.jar

USER authapi

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]