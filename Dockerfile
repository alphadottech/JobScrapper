# Stage 1: Build the application using Maven and Java 17
FROM maven:latest AS build


# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies (to avoid re-downloading on every change)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the entire project source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application using openjdk:17-slim
FROM openjdk:17-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file built in Stage 1 to the new image
COPY --from=build /app/target/purelyprep-1.0-jar-with-dependencies.jar /app/purelyprep-1.0-jar-with-dependencies.jar

# Expose the application port
EXPOSE 8087

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/purelyprep-1.0-jar-with-dependencies.jar"]
