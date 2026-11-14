# Multi-arch (amd64 + arm64, e.g. Raspberry Pi): all base images are official multi-arch images.
# Used by docker-compose.yml; build alone with: docker build -t planetswitch .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.vaadin \
    ./mvnw -B clean package -Pproduction -DskipTests

FROM eclipse-temurin:21-jre
# python3 runs tools/*.py (player analysis from the graph context menu); they only use the stdlib
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
COPY tools/*.py tools/
# data/ (archives, analyses) and planets-credentials.properties are mounted by docker-compose.yml
VOLUME /app/data
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
