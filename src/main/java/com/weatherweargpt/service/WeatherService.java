package com.weatherweargpt.service;

import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.entity.WeatherEntity;
import com.weatherweargpt.repository.UserRepository;
import com.weatherweargpt.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {
    private final WeatherRepository weatherRepository;
    private final UserRepository userRepository;

    public WeatherEntity saveWeather(WeatherEntity weather){
        return weatherRepository.save(weather);
    }

    //UserEntity에서 userId 가져오기
    public UserEntity findUserById(Long userId){
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new RuntimeException("해당 userId를 가진 사용자를 찾을 수 없습니다.");
        }
    }


}
