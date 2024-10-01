FROM openjdk:17-jdk-alpine
VOLUME /tmp
COPY target/weather-wear-gpt-server.jar /weather-wear-gpt-server.jar
ENTRYPOINT ["java","-jar","/weather-wear-gpt-server.jar", "--spring.profiles.active=prod"]