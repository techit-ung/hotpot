FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

COPY gradle/          gradle/
COPY gradlew          gradlew
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts
COPY src/             src/
COPY standalone/      standalone/

RUN ./gradlew :standalone:installDist --no-daemon --quiet

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /build/standalone/build/install/standalone/ /app/

EXPOSE 8080

ENTRYPOINT ["/app/bin/standalone"]
