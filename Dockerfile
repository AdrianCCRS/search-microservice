FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies first (caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache netcat-openbsd

COPY wait-for-services.sh .
RUN chmod +x wait-for-services.sh

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/search-microservice-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["./wait-for-services.sh"]
