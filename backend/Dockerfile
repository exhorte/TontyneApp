# --- Etape de compilation ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dependances mises en cache separement du code source
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Etape d'execution ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Utilisateur non privilegie
RUN addgroup -S tontyn && adduser -S tontyn -G tontyn
COPY --from=build /app/target/*.jar app.jar
RUN chown tontyn:tontyn app.jar
USER tontyn

# Render (et la plupart des PaaS) fournissent le port d'ecoute via $PORT.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT} --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
