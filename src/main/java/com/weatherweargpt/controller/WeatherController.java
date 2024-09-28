package com.weatherweargpt.controller;

import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.entity.WeatherEntity;
import com.weatherweargpt.service.WeatherDataService;
import com.weatherweargpt.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherDataService weatherDataService;
    private final WeatherService weatherService;

    /**
     * 지역명으로 Location Key 가져오기 (텍스트 기반 검색)
     * 입력된 지역명을 사용하여 AccuWeather의 텍스트 검색 API를 통해 Location Key를 가져옵니다.
     */
    @GetMapping("/location")
    public ResponseEntity<?> getLocationKey(@RequestParam String location) {
        try {
            String locationKey = weatherDataService.getLocationKey(location);
            return ResponseEntity.ok(locationKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류: " + e.getMessage());
        }
    }

    /**
     * 위도/경도 기반 Location Key 가져오기
     * 사용자가 위도와 경도를 입력하면, 해당 위치에 대한 Location Key를 AccuWeather의 Geoposition API를 사용하여 가져옵니다.
     */
    @GetMapping("/location/geoposition")
    public ResponseEntity<?> getLocationKeyByGeoposition(@RequestParam double latitude, @RequestParam double longitude) {
        try {
            String locationKey = weatherDataService.getLocationKeyByGeoposition(latitude, longitude);
            return ResponseEntity.ok(locationKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류: " + e.getMessage());
        }
    }

    /**
     * 현재 날씨와 5일간의 날씨 예보 데이터 저장
     * 입력된 지역명(location)을 기반으로 Location Key를 가져오고, 해당 Location Key를 사용하여 날씨 정보를 가져온 후 DB에 저장합니다.
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveWeatherData(@RequestParam String location, @RequestParam Long userId) {
        try {
            // 1. Location key 가져오기
            String locationKey = weatherDataService.getLocationKey(location);

            // 2. 현재 날씨 정보 가져오기
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);

            // 3. 5일간의 날씨 예보 정보 가져오기
            JSONArray dailyForecast = weatherDataService.getDailyForecast(locationKey);

            // 4. userEntity 가져오기
            UserEntity user = weatherService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 userId를 가진 사용자를 찾을 수 없습니다");
            }

            // 5. 날씨 데이터를 엔티티로 변환하여 저장
            WeatherEntity weather = new WeatherEntity();
            weather.setUserEntity(user); // 사용자 엔티티 설정
            weather.setWeatherText(currentWeather.getString("WeatherText")); // WeatherText는 단일 문자열
            weather.setTemperature(currentWeather.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value"));
            weather.setRelativeHumidity(currentWeather.getInt("RelativeHumidity"));
            weather.setWind(currentWeather.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value"));
            weather.setHasPrecipitation(currentWeather.getBoolean("HasPrecipitation"));

            // AccuWeather에서 관측된 날짜 시간 정보 사용 (OffsetDateTime으로 변환)
            String observationDateTimeStr = currentWeather.getString("LocalObservationDateTime");
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(observationDateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // OffsetDateTime을 LocalDateTime으로 변환하여 저장
            LocalDateTime observationDateTime = offsetDateTime.toLocalDateTime();
            weather.setWeatherDate(observationDateTime);

            // DB에 데이터 저장
            WeatherEntity savedWeather = weatherService.saveWeather(weather);

            // 저장된 Weather 객체 반환
            return ResponseEntity.ok(savedWeather);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 Location Key에 대한 현재 날씨 정보 가져오기
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentWeather(@RequestParam String locationKey) {
        try {
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);
            return ResponseEntity.ok(currentWeather.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("현재 날씨 정보를 가져오는 데 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 Location Key에 대한 5일간의 날씨 예보 정보 가져오기
     */
    @GetMapping("/forecast")
    public ResponseEntity<?> getDailyForecast(@RequestParam String locationKey) {
        try {
            JSONArray dailyForecast = weatherDataService.getDailyForecast(locationKey);
            return ResponseEntity.ok(dailyForecast.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("날씨 예보 정보를 가져오는 데 실패했습니다: " + e.getMessage());
        }
    }
}

