FROM openjdk:17
VOLUME /tmp
ENTRYPOINT ["java","-jar","/weather-wear-gpt-server.jar","--spring.profiles.active=prod"]