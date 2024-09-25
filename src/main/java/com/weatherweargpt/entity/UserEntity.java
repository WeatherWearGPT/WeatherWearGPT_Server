package com.weatherweargpt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    private String password;

    private String name;

    private String email;

    private String gender;

    private String height;

    private String weight;

    private String role;
}