package com.weatherweargpt.jwt;

import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import com.weatherweargpt.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        logger.info("Attempting authentication for user: {}", username);

        if (username == null || password == null) {
            logger.error("Username or password is null");
            throw new AuthenticationException("Username or password is missing") {};
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);
        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        logger.info("Fetching user details for: {}", customUserDetails.getUsername());
        UserEntity userEntity = userRepository.findByUserName(customUserDetails.getUsername());
        if (userEntity == null) {
            logger.error("User not found for username: {}", customUserDetails.getUsername());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = jwtUtil.createJwt(userEntity, 60*60*1000L);
        response.addHeader("Authorization", "Bearer " + token);
        response.setStatus(HttpServletResponse.SC_OK);
        logger.info("Authentication successful for user: {}", customUserDetails.getUsername());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        logger.error("Authentication failed: {}", failed.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}