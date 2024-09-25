package com.weatherweargpt.jwt;

import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {
    private final UserRepository userRepository;
    private SecretKey secretKey;

    public JWTUtil(UserRepository userRepository, @Value("${spring.jwt.secret}")String secret) {
        this.userRepository = userRepository;
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public UserEntity getUser(String token) {
        // JWT에서 username 추출
        String username = getUsername(token);
        // username을 사용하여 UserEntity를 찾음
        UserEntity user = userRepository.findByUsername(username);

        // user가 null인지 확인
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // 추가로, 사용자 ID도 확인 (선택 사항)
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        return user;
    }

    public String getUsername(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Boolean isExpired(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String createJwt(UserEntity user, Long expiredMs) {

        return Jwts.builder()
                .claim("Id", user.getId() )
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

}