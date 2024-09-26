package com.weatherweargpt.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class  UserDTO {
    private Long userId;  // UserEntity의 userId와 일치하도록 수정

    private String role;

    private String name;

    private String userName;  // UserEntity의 userName과 일치하도록 수정
}
