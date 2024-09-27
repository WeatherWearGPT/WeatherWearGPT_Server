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

    private String weatherText;
    private double temperature;
    private int relativeHumidity;
    private double wind;
    private boolean hasPrecipitation;
    private LocalDateTime weatherDate;

}
