# syntax=docker/dockerfile:1

# I use a multi-stage build so my final image stays small:
# - Stage 1 builds my WAR with Maven + Java 11
# - Stage 2 runs it on Tomcat 9 (Java 11)

# -------- Build stage --------
FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

# I copy my pom.xml first so Docker can cache dependency downloads.
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# I then copy my source and build the WAR.
COPY src ./src
RUN mvn -B -DskipTests package

# -------- Runtime stage --------
FROM tomcat:9.0-jdk11-temurin

# I remove the default Tomcat apps so only my service is deployed.
RUN rm -rf /usr/local/tomcat/webapps/*

# I deploy as ROOT.war so my base URL is http://localhost:8080/ (no /Part_B prefix).
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# I expose port 8080 because Tomcat listens there by default.
EXPOSE 8080

# I start Tomcat in the foreground for container logging.
CMD ["catalina.sh", "run"]
