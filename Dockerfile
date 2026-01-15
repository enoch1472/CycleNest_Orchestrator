# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

# Copy Maven config first for better layer caching
COPY pom.xml .
COPY src ./src

# Build WAR (skip tests for faster container builds)
RUN mvn -DskipTests package


# ---------- RUNTIME STAGE ----------
FROM tomcat:9.0-jdk11-temurin

# Optional but useful for health checks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy WAR as ROOT so URLs are clean: http://localhost:8080/api/...
COPY --from=build /app/target/cycle-nest-orchestrator.war /usr/local/tomcat/webapps/ROOT.war

# Expose Tomcat port
EXPOSE 8080

# Basic healthcheck (adjust endpoint if you prefer a different one)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/orchestrator/items?available=true || exit 1
