package com.weatherweargpt.service;

import com.weatherweargpt.entity.WeatherEntity;
import com.weatherweargpt.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {
    private final WeatherRepository weatherRepository;

    public WeatherEntity saveWeather(WeatherEntity weather){
        return weatherRepository.save(weather);
    }
}
