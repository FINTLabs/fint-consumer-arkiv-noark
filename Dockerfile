FROM eclipse-temurin:8-jdk AS builder
WORKDIR /workspace
COPY . .
ARG apiVersion
ARG buildFlags=""
RUN ./gradlew --no-daemon ${buildFlags} -PapiVersion=${apiVersion} build

FROM eclipse-temurin:8-jre
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"
COPY --from=builder /workspace/build/deps/external/*.jar /data/
COPY --from=builder /workspace/build/deps/fint/*.jar /data/
COPY --from=builder /workspace/build/libs/fint-consumer-arkiv-noark-*.jar /data/fint-consumer-arkiv-noark.jar
ENTRYPOINT ["java", "-jar", "/data/fint-consumer-arkiv-noark.jar"]
