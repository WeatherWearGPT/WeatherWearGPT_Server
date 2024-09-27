package com.weatherweargpt.oauth2;

import com.weatherweargpt.dto.CustomOAuth2User;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.jwt.JWTUtil;
import com.weatherweargpt.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public CustomSuccessHandler(JWTUtil jwtUtil, UserRepository userRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        UserEntity userEntity = userRepository.findByUserName(customUserDetails.getUsername());

        String token = jwtUtil.createJwt(userEntity, 60*60*1000L);

        response.addCookie(createCookie("Authorization", token));

        response.setHeader("Authorization", "Bearer " + token);

        // 사용자가 데이터베이스에 존재하지 않으면 추가적인 회원가입 화면으로 리다이렉트
        if (userEntity.getHeight() == null || userEntity.getWeight() == null) {
            response.sendRedirect("http://localhost:3000/social-signup"); // 회원가입 페이지 URL
        } else {
            response.sendRedirect("http://localhost:3000/chat"); // 로그인 후 홈 화면 URL
        }
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(30*60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}