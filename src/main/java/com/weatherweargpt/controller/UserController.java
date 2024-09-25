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

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@AuthUser UserEntity user) {
        System.out.println(user.getUsername());
        if(user == null) {
            System.out.println("user가 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = user.getId();
        JoinDTO joinDTO = userService.getAll(userId);
        return ResponseEntity.ok(joinDTO);
    }

    @PatchMapping("/user/{id}")
    public ResponseEntity<?> updateUser(@PathVariable(value = "id") Long userId, @AuthUser UserEntity user, @RequestPart(value = "data") JoinDTO joinDTO) {
        if(user == null) {
            System.out.println("user가 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!user.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("본인의 마이 페이지만 수정할 수 있습니다.");
        }

        long updatedUserId = userService.updateUser(user, joinDTO);

        if (updatedUserId != user.getId() || updatedUserId == -1) {
            return ResponseEntity.internalServerError().body("회원 정보 수정에 실패하였습니다.");
        }

        return ResponseEntity.ok(updatedUserId + ": 정보가 변경되었습니다.");
    }
}