# Base image with JDK 17
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the build output (JAR file) from the host to the container
COPY app.jar app.jar

# Expose the port on which the Spring app will run
EXPOSE 8080

# Command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]