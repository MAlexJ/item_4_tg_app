FROM eclipse-temurin:26-jdk-alpine AS builder
WORKDIR /application
COPY . .
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew clean build -x test

FROM eclipse-temurin:26-jdk-alpine AS layers
WORKDIR /application
COPY --from=builder /application/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:26-jdk-alpine
VOLUME /tmp
RUN adduser -S spring-user
USER spring-user
WORKDIR /application

COPY --from=layers /application/extracted/spring-boot-loader/ ./
COPY --from=layers /application/extracted/dependencies/BOOT-INF/lib/ ./BOOT-INF/lib/
COPY --from=layers /application/extracted/snapshot-dependencies/ ./BOOT-INF/snapshot-lib/
COPY --from=layers /application/extracted/application/ ./

ENV JAVA_OPTS="-XX:+UseG1GC -XX:+TieredCompilation -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
