# Base image with JDK 17
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY build/libs/tamago-0.0.1-SNAPSHOT.jar .

# Expose the application port
EXPOSE 8080

# Run the Spring app
ENTRYPOINT ["java", "-jar", "tamago-0.0.1-SNAPSHOT.jar"]