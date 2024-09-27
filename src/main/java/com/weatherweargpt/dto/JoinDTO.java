package com.weatherweargpt.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinDTO {
    private Long userId;

    private String userName;  // UserEntity의 userName 필드에 맞춤

    private String userPassword;  // UserEntity의 userPassword 필드에 맞춤

    private String name;

    private String email;

    private String gender;

    private Integer height;  // UserEntity와 동일하게 Integer 타입으로 변경

    private Integer weight;  // UserEntity와 동일하게 Integer 타입으로 변경

    //private String role;  // UserEntity의 role 필드 추가 (선택사항)
}
