# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Descargar dependencias primero (cache layer)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compilar aplicación
COPY src src
RUN ./mvnw package -DskipTests -B

# Extraer capas del fat JAR para caching óptimo
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# Usuario no-root (crear antes de copiar para no duplicar capas con chown)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar capas Spring Boot (de menos a más cambiante = mejor cache)
COPY --from=builder --chown=appuser:appgroup /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/target/extracted/application/ ./

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=10s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
