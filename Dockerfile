FROM openjdk:17
VOLUME /tmp
COPY build/libs/WeatherWearGPT.jar weather-wear-gpt-server.jar
ENTRYPOINT ["java","-jar","/weather-wear-gpt-server.jar"]