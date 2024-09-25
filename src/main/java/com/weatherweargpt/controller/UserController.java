package com.weatherweargpt.controller;

import com.weatherweargpt.config.AuthUser;
import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.dto.UserDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import com.weatherweargpt.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    @PostMapping("/register")
    public ResponseEntity<?> join(JoinDTO joinDTO) {
        if (userRepository.existsByUsername(joinDTO.getUsername())) {
           return  ResponseEntity.badRequest().body("이미 존재하는 회원입니다.");
        }
        userService.join(joinDTO);
        return ResponseEntity.ok(joinDTO);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthUser UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.delete(user.getId());

        return ResponseEntity.ok(user.getId() + " 회원님이 회원 탈퇴하였습니다.");
    }


}