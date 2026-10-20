# 多阶段构建：云端从 GitHub 拉代码后自行编译，不依赖本地 target/
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/wordrecall-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# 预生成的单词发音（14296 个，约 138MB），已随仓库提交
COPY data/audio /app/data/audio

ENV SERVER_PORT=8080 \
    APP_AUDIO_DIR=/app/data/audio \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
