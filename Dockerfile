# Base image with JDK 17
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the build.gradle and gradle wrapper files
COPY build.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew build -x test --no-daemon

# Copy the Spring app code
COPY . .

# Package the application
RUN ./gradlew bootJar --no-daemon

# Expose the application port
EXPOSE 8080

# Run the Spring app
ENTRYPOINT ["java", "-jar", "build/libs/tamago-0.0.1-SNAPSHOT.jar"]