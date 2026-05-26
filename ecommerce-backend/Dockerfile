# ============================================================
# Multi-stage Dockerfile for E-Commerce Backend
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE image
# ============================================================

# ===== STAGE 1: BUILD =====
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (for layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer if pom.xml unchanged)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S ecommerce && adduser -S ecommerce -G ecommerce

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R ecommerce:ecommerce /app

# Switch to non-root user
USER ecommerce

# Expose application port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
