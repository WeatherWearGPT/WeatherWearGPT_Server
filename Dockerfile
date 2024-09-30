FROM openjdk:17
VOLUME /tmp
COPY build/libs/WeatherWearGPT-0.0.1-SNAPSHOT.jar weather-wear-gpt-server.jar
ENTRYPOINT ["java","-jar","/weather-wear-gpt-server.jar"]