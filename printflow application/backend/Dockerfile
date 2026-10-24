FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon test bootJar

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /build/build/libs/printflow-api.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
