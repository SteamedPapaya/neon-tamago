# Base image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the build output from Gradle
COPY build/libs/*.jar app.jar

# Expose the port that the Spring app will run on
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]