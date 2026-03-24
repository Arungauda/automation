# Development Dockerfile - simplified for local development
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy gradle wrapper and configuration files
COPY gradlew gradlew.bat gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew build --no-daemon --dry-run

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test --no-daemon

# Create cache directory for EhCache
RUN mkdir -p /app/cache/ehcache

# Set JVM options for development
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseG1GC"

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar build/libs/*.jar"]
