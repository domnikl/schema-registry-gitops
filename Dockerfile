FROM amazoncorretto:17.0.15-alpine AS tester

WORKDIR /home/cuser

COPY ./gradle /home/cuser/gradle
COPY ./gradlew ./gradle.properties ./build.gradle.kts ./settings.gradle.kts ./.editorconfig /home/cuser/
RUN ./gradlew --no-daemon build
COPY ./src /home/cuser/src
RUN ./gradlew --no-daemon check

FROM tester AS builder
RUN ./gradlew --no-daemon shadowJar

FROM amazoncorretto:17.0.15-alpine AS distribution
COPY --from=builder /home/cuser/build/libs/schema-registry-gitops.jar /home/cuser/schema-registry-gitops.jar

WORKDIR /home/cuser

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "schema-registry-gitops.jar"]
