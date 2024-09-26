package com.weatherweargpt.controller;

import com.weatherweargpt.entity.WeatherEntity;
import com.weatherweargpt.service.WeatherDataService;
import com.weatherweargpt.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

// AccuWeather API의 Locations API, Forecast API, Current Conditions API Fetch
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {
    @Value("${accuweather.api.key}")
    private String accuweatherApiKey;

    private final RestTemplate restTemplate;
    private final WeatherService weatherService;
    private final WeatherDataService weatherDataService;

    //Location API를 통해 해당 지역의 locationKey를 가져옴
    @GetMapping("/location")
    public ResponseEntity<?> getLocationKey(@RequestParam String location) {
        // AccuWeather Locations API URL
        String url = "http://dataservice.accuweather.com/locations/v1/cities/search?apikey=" + accuweatherApiKey + "&q=" + location;

        try {
            // API 요청 보내기
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // JSON 응답 파싱
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONArray jsonArray = new JSONArray(response.getBody());
                if (jsonArray.length() > 0) {
                    // 첫 번째 결과에서 locationKey 가져오기
                    JSONObject firstResult = jsonArray.getJSONObject(0);
                    String locationKey = firstResult.getString("Key");

                    // locationKey 반환
                    return ResponseEntity.ok(locationKey);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 위치를 찾을 수 없습니다");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Location key를 가져오는 데 실패했습니다");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }

    //현재 날씨와 5일간의 날씨 예보 데이터 저장

    @PostMapping("/save")
    public ResponseEntity<?> saveWeatherData(@RequestParam String location){
        try {
            // Location key 가져오기
            String locationKey = weatherDataService.getLocationKey(location);

            // 현재 날씨 정보 가져오기
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);

            // 날씨 예보 정보 가져오기
            JSONArray dailyForecast = weatherDataService.getDailyForecast(locationKey);

            // 날씨 데이터를 엔티티로 변환
            WeatherEntity weather = new WeatherEntity();
            weather.setUserId(1L); // 임의로 userId를 설정
            weather.setWeatherText(currentWeather.getString("WeatherText")); // WeatherText는 단일 문자열
            weather.setTemperature(currentWeather.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value"));
            weather.setRelativeHumidity(currentWeather.getInt("RelativeHumidity"));
            weather.setWind(currentWeather.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value"));
            weather.setHasPrecipitation(currentWeather.getBoolean("HasPrecipitation"));

            // AccuWeather에서 관측된 날짜 시간 정보 사용 (OffsetDateTime으로 변환)
            String observationDateTimeStr = currentWeather.getString("LocalObservationDateTime");
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(observationDateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // OffsetDateTime을 LocalDateTime으로 변환
            LocalDateTime observationDateTime = offsetDateTime.toLocalDateTime();

            // 해당 시간을 Weather 엔티티에 저장
            weather.setWeatherDate(observationDateTime);

            // 데이터를 DB에 저장
            WeatherEntity savedWeather = weatherService.saveWeather(weather);

            // 저장된 Weather 객체 반환
            return ResponseEntity.ok(savedWeather);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }


}

