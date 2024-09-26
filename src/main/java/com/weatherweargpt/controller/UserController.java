package com.weatherweargpt.controller;

import com.weatherweargpt.config.AuthUser;
import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import com.weatherweargpt.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
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
        System.out.println("Withdraw request for ID: " + user.getId());
        if (user == null) {
            System.out.println("user가 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = user.getId();
        userService.delete(userId);

        return ResponseEntity.ok(user.getId() + " 회원님이 회원 탈퇴하였습니다.");
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthUser UserEntity user) {
        System.out.println(user.getUsername());
        if(user == null) {
            System.out.println("user가 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = user.getId();
        JoinDTO joinDTO = userService.getAll(userId);
        return ResponseEntity.ok(joinDTO);
    }
}