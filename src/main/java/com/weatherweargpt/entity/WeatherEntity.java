package com.weatherweargpt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long weatherId;

    private long userId;
    private String weatherText;
    private double temperature;
    private int relativeHumidity;
    private double wind;
    private boolean hasPrecipitation;
    private LocalDateTime weatherDate;

}
