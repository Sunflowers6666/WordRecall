FROM eclipse-temurin:17-jre

WORKDIR /app

# 先本地构建 jar：cd backend && mvn -B package -DskipTests
COPY target/wordrecall-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# 预生成的单词发音（约 138MB）。本地构建时必须存在；
# 若改用对象存储托管音频，可删掉这一行并改 AudioService。
COPY data/audio /app/data/audio

ENV SERVER_PORT=8080 \
    APP_AUDIO_DIR=/app/data/audio \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
