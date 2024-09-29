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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherDataService weatherDataService;
    private final WeatherService weatherService;

    @GetMapping("/location")
    public ResponseEntity<?> getLocationKey(@RequestParam String location) {
        try {
            String locationKey = weatherDataService.getLocationKey(location);
            return ResponseEntity.ok(locationKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류: " + e.getMessage());
        }
    }

    @GetMapping("/location/geoposition")
    public ResponseEntity<?> getLocationKeyByGeoposition(@RequestParam double latitude, @RequestParam double longitude) {
        try {
            String locationKey = weatherDataService.getLocationKeyByGeoposition(latitude, longitude);
            return ResponseEntity.ok(locationKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveWeatherData(@RequestParam String location, @RequestParam Long userId, @RequestParam(required = false) String dateTime) {
        try {
            String locationKey = weatherDataService.getLocationKey(location);

            LocalDate targetDate = (dateTime != null) ? LocalDate.parse(dateTime) : LocalDate.now();
            JSONObject weatherData = weatherDataService.getWeatherForDate(locationKey, targetDate);

            UserEntity user = weatherService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 userId를 가진 사용자를 찾을 수 없습니다");
            }

            WeatherEntity weather = new WeatherEntity();
            weather.setUserEntity(user);
            weather.setWeatherText(weatherData.getJSONObject("Day").getString("IconPhrase"));
            weather.setTemperature(weatherData.getJSONObject("Temperature").getJSONObject("Maximum").getDouble("Value"));

            // RelativeHumidity 처리 수정
            JSONObject dayForecast = weatherData.getJSONObject("Day");
            if (dayForecast.has("RelativeHumidity")) {
                Object humidityValue = dayForecast.get("RelativeHumidity");
                if (humidityValue instanceof Integer) {
                    weather.setRelativeHumidity((Integer) humidityValue);
                } else if (humidityValue instanceof Double) {
                    weather.setRelativeHumidity(((Double) humidityValue).intValue());
                } else {
                    weather.setRelativeHumidity(0); // 기본값 설정 또는 다른 처리 방법 선택
                }
            } else {
                weather.setRelativeHumidity(0); // RelativeHumidity 필드가 없는 경우 기본값 설정
            }

            weather.setWind(weatherData.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value"));
            weather.setHasPrecipitation(weatherData.getJSONObject("Day").getBoolean("HasPrecipitation"));
            weather.setWeatherDate(targetDate.atStartOfDay());

            WeatherEntity savedWeather = weatherService.saveWeather(weather);
            return ResponseEntity.ok(savedWeather);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentWeather(@RequestParam String locationKey) {
        try {
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);
            return ResponseEntity.ok(currentWeather.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("현재 날씨 정보를 가져오는 데 실패했습니다: " + e.getMessage());
        }
    }

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