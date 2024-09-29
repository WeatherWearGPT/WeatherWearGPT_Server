package com.weatherweargpt.service;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WeatherDataService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherDataService.class);

    @Value("${accuweather.api.key}")
    private String accuweatherApiKey;

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate;

    public String getLocationKey(String location) {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/search?apikey=" + accuweatherApiKey + "&q=" + location;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONArray jsonArray = new JSONArray(response.getBody());
            if (jsonArray.length() > 0) {
                JSONObject firstResult = jsonArray.getJSONObject(0);
                return firstResult.getString("Key");
            }
        }

        double[] coordinates = getLatitudeLongitude(location);
        if (coordinates != null) {
            return getLocationKeyByGeoposition(coordinates[0], coordinates[1]);
        }

        throw new RuntimeException("Location key를 가져오지 못했습니다.");
    }

    public String getLocationKeyByGeoposition(double latitude, double longitude) {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=" + accuweatherApiKey + "&q=" + latitude + "," + longitude;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject jsonObject = new JSONObject(response.getBody());
            if (jsonObject.has("Key")) {
                return jsonObject.getString("Key");
            }
        }

        throw new RuntimeException("Geoposition을 통한 Location Key를 가져오지 못했습니다.");
    }

    public double[] getLatitudeLongitude(String location) {
        try {
            String geocodeUrl = String.format(
                    "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                    location, googleApiKey
            );

            logger.debug("Google Maps Geocoding API 호출 URL: {}", geocodeUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(geocodeUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = new JSONObject(response.getBody());

                if ("OK".equals(jsonResponse.getString("status"))) {
                    JSONArray results = jsonResponse.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject locationObject = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location");
                        double latitude = locationObject.getDouble("lat");
                        double longitude = locationObject.getDouble("lng");

                        logger.info("Google Maps에서 '{}'의 위도: {}, 경도: {} 조회 성공", location, latitude, longitude);
                        return new double[]{latitude, longitude};
                    } else {
                        logger.warn("Google Maps에서 '{}'에 대한 결과를 찾지 못했습니다.", location);
                    }
                } else {
                    logger.error("Geocoding API 호출 실패. 상태: {}, 에러 메시지: {}", jsonResponse.getString("status"), jsonResponse.optString("error_message"));
                }
            } else {
                logger.error("Geocoding API 요청 실패. 상태 코드: {}, 응답 내용: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Google Maps Geocoding API 호출 중 오류가 발생했습니다. 지역명: {}, 오류: {}", location, e.getMessage());
        }

        logger.warn("해당 지역에 대한 좌표를 찾을 수 없습니다. 지역명: {}", location);
        return null;
    }

    public JSONObject getCurrentWeather(String locationKey) {
        String url = "http://dataservice.accuweather.com/currentconditions/v1/" + locationKey + "?apikey=" + accuweatherApiKey + "&details=true";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONArray jsonArray = new JSONArray(response.getBody());
            return jsonArray.getJSONObject(0);
        }
        throw new RuntimeException("Current weather data 불러오기 실패");
    }

    public JSONArray getDailyForecast(String locationKey) {
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey + "?apikey=" + accuweatherApiKey + "&details=true";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject forecast = new JSONObject(response.getBody());
            return forecast.getJSONArray("DailyForecasts");
        }
        throw new RuntimeException("Weather forecast data 불러오기 실패");
    }

    public JSONObject getWeatherForDate(String locationKey, LocalDate targetDate) {
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey + "?apikey=" + accuweatherApiKey + "&details=true";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject forecast = new JSONObject(response.getBody());
            JSONArray dailyForecasts = forecast.getJSONArray("DailyForecasts");

            for (int i = 0; i < dailyForecasts.length(); i++) {
                JSONObject dailyForecast = dailyForecasts.getJSONObject(i);
                String forecastDate = dailyForecast.getString("Date");
                LocalDate forecastLocalDate = LocalDate.parse(forecastDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                if (forecastLocalDate.equals(targetDate)) {
                    logger.info("Found weather data for date: {}", targetDate);
                    return dailyForecast;
                }
            }
        }
        throw new RuntimeException("Weather data for the specified date could not be retrieved");
    }
}