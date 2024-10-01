FROM openjdk:17
VOLUME /tmp
COPY build/libs/weather-wear-gpt-server.jar /app/weather-wear-gpt-server.jar
ENTRYPOINT ["java", "-jar", "/app/weather-wear-gpt-server.jar", "--spring.profiles.active=prod"]