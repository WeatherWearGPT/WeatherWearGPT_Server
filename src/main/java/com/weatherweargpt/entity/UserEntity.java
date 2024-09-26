package com.weatherweargpt.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")  // Primary Key (PK)
    private Long userId;

    @Column(name = "UserName", nullable = false, unique = true)
    private String userName;  // 사용자명

    @Column(name = "UserPassword")
    private String userPassword;  // 사용자 비밀번호

    @Column(name = "name")
    private String name;  // 이름

    @Column(name = "Email")
    private String email;  // 이메일

    @Column(name = "Gender")
    private String gender;  // 성별

    @Column(name = "Height")
    private Integer height;  // 키

    @Column(name = "Weight")
    private Integer weight;  // 몸무게

    @Column(name = "role")
    private String role;  // 역할 (UserEntity에서 추가된 필드)

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference  // 순환 참조 방지
    private List<Dialogue> dialogues;  // 유저와 연결된 대화 목록
}
