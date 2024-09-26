package com.weatherweargpt.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final UserDTO userDTO;

    public CustomOAuth2User(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userDTO.getRole()));
    }

    @Override
    public String getName() {
        return userDTO.getName();
    }

    // 필드 이름에 맞게 getUsername() 수정
    public String getUsername() {
        return userDTO.getUserName();  // 수정된 부분: getUsername()에서 getUserName()으로 변경
    }
}

