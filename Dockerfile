FROM openjdk:17
WORKDIR /app
# 소스 코드를 복사
COPY . .
# Gradle 빌드를 수행하여 JAR 파일 생성
RUN ./gradlew build
# JAR 파일을 복사
COPY build/libs/WeatherWearGPT.jar weather-wear-gpt-server.jar
# 실행
ENTRYPOINT ["java", "-jar", "/weather-wear-gpt-server.jar", "--spring.profiles.active=prod"]