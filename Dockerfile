# Dockerfile a la racine du depot : c'est ici que Render (build Docker par
# defaut) le cherche, meme si le code source du backend vit dans backend/.
# Pour un build local depuis backend/, utiliser backend/Dockerfile a la place.

# --- Etape de compilation ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
RUN mvn -B -q package -DskipTests

# --- Etape d'execution ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S tontyn && adduser -S tontyn -G tontyn
COPY --from=build /app/target/*.jar app.jar
RUN chown tontyn:tontyn app.jar
USER tontyn

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT} --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
