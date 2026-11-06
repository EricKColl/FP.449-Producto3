# Etapa 1: compilación con Maven y JDK 21
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

# Se descargan primero las dependencias para aprovechar la caché de capas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: ejecución con una JRE 21 ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S biblioteca && adduser -S biblioteca -G biblioteca
COPY --from=build --chown=biblioteca:biblioteca /app/target/*.jar app.jar
USER biblioteca

EXPOSE 8080

# /actuator/health solo responde UP si la aplicación también llega a la base de datos
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# La JVM ajusta la memoria al límite del contenedor
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
