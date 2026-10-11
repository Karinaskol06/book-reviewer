# Build the Spring Boot jar inside Docker so `docker compose up --build` works
# without a pre-built target/ on the host.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
