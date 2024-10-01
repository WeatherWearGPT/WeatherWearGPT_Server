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


    public JWTUtil(UserRepository userRepository, @Value("${spring.jwt.secret}") String secret) {
        this.userRepository = userRepository;
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public UserEntity getUser(String token) {
        String username = getUsername(token);

        UserEntity user = userRepository.findByUserName(username);  // 수정된 부분: findByUsername() → findByUserName()

        System.out.println(user.getUserName() + "getUser");  // 수정된 부분: getUsername() → getUserName()

        // 유저를 찾을 수 없는 경우
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // ID 체크
        if (user.getUserId() == null) {  // 수정된 부분: getId() → getUserId()

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
                .claim("id", user.getUserId())  // 수정된 부분: getId() → getUserId()
                .claim("username", user.getUserName())  // 수정된 부분: getUsername() → getUserName()
                .claim("role", user.getRole())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }
}
