package com.weatherweargpt.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinDTO {
    private String username;

    private String password;

    private String name;

    private String email;

    private String gender;

    private String height;

    private String weight;
}
