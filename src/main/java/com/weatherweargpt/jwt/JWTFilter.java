package com.weatherweargpt.jwt;

import com.weatherweargpt.dto.CustomOAuth2User;
import com.weatherweargpt.dto.UserDTO;
import com.weatherweargpt.entity.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // Authorization 헤더가 없으면 쿠키에서 시도
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("Authorization".equals(cookie.getName())) {
                        authorization = "Bearer " + cookie.getValue();
                        break;
                    }
                }
            }
        }


        // 토큰이 없거나 만료된 경우
        if (authorization == null || !authorization.startsWith("Bearer ") || jwtUtil.isExpired(authorization.substring(7))) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " 문자열 제거 후 토큰 추출
        String token = authorization.substring(7);

        UserEntity user;
        try {
            user = jwtUtil.getUser(token);
        } catch (IllegalArgumentException e) {
            System.out.println("User not found: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Unauthorized
            return ; // 처리 종료
        }

        // UserDTO를 생성하여 값 세팅
        UserDTO userDTO = new UserDTO();

        userDTO.setUserId(user.getUserId());  // 수정된 부분: setId() → setUserId()
        userDTO.setUserName(user.getUserName());  // 수정된 부분: setUsername() → setUserName()
        userDTO.setRole(user.getRole());

        // UserDetails에 회원 정보 객체 담기
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

        System.out.println(customOAuth2User.getUsername());

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        System.out.println(authToken.getAuthorities());

        // 세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}

