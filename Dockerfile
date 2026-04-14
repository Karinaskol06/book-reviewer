FROM maven:3.9-eclipse-temurin-22 AS build
WORKDIR /app

# Copy only POM first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code (this layer changes frequently)
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:22-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]