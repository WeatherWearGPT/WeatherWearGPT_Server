package com.weatherweargpt.service;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WeatherDataService {

    @Value("${accuweather.api.key}")
    private String accuweatherApiKey;

    private final RestTemplate restTemplate;

    // LocationKey를 가져오는 메서드
    public String getLocationKey(String location) {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/search?apikey=" + accuweatherApiKey + "&q=" + location;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONArray jsonArray = new JSONArray(response.getBody());
            if (jsonArray.length() > 0) {
                // 첫 번째 결과에서 locationKey 가져오기
                JSONObject firstResult = jsonArray.getJSONObject(0);
                return firstResult.getString("Key");
            }
        }

        throw new RuntimeException("Location key를 가져오지 못했습니다.");
    }

    //현재 날씨 데이터 가져오는 메서드
    public JSONObject getCurrentWeather(String locationKey) {
        String url = "http://dataservice.accuweather.com/currentconditions/v1/" + locationKey + "?apikey=" + accuweatherApiKey+ "&details=true";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONArray jsonArray = new JSONArray(response.getBody());
            return jsonArray.getJSONObject(0);
        }
        throw new RuntimeException("Current weather data 불러오기 실패");
    }

    //날씨 예보 데이터 가져오는 메서드
    public JSONArray getDailyForecast(String locationKey) {
        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey + "?apikey=" + accuweatherApiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject forecast = new JSONObject(response.getBody());
            return forecast.getJSONArray("DailyForecasts");
        }
        throw new RuntimeException("Weather forecast data 불러오기 실패");
    }

}
