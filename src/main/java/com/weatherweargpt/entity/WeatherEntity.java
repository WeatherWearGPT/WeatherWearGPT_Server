package com.weatherweargpt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_id")
    private long weatherId;

    //외래키 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "UserId", foreignKey = @ForeignKey(name = "FK_User_Weather"))
    private UserEntity userEntity;

    @Column(name = "weather_text")
    private String weatherText;

    private double temperature;

    @Column(name = "relative_humidity")
    private int relativeHumidity;

    private double wind;

    @Column(name = "has_precipitation")
    private boolean hasPrecipitation;

    @Column(name = "weather_date")
    private LocalDateTime weatherDate;

}
