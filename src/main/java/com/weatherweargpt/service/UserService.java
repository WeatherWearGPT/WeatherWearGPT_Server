package com.weatherweargpt.service;

import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void join(JoinDTO joinDTO) {
        String username = joinDTO.getUserName();
        String password = joinDTO.getUserPassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String name = joinDTO.getName();
        String email = joinDTO.getEmail();
        String gender = joinDTO.getGender();
        Integer height = joinDTO.getHeight();
        Integer weight = joinDTO.getWeight();

        UserEntity data = new UserEntity();
        data.setUserName(username);
        data.setUserPassword(bCryptPasswordEncoder.encode(password));
        data.setName(name);
        data.setEmail(email);
        data.setGender(gender);
        data.setHeight(height);
        data.setWeight(weight);
        data.setRole("ROLE_USER");

        userRepository.save(data);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public JoinDTO getAll(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        JoinDTO joinDTO = new JoinDTO();
        joinDTO.setUserId(user.getUserId());
        joinDTO.setUserName(user.getUserName());
        joinDTO.setName(user.getName());
        joinDTO.setHeight(user.getHeight());
        joinDTO.setWeight(user.getWeight());
        joinDTO.setGender(user.getGender());
        joinDTO.setEmail(user.getEmail());
        return joinDTO;
    }

    public long updateUser(UserEntity user, JoinDTO joinDTO) {
        try {
//            // 비밀번호 업데이트
//            String password = joinDTO.getUserPassword();
//            if (password != null && !password.isBlank()) {
//                user.setUserPassword(bCryptPasswordEncoder.encode(password));
//            }
//
//            // 이름 업데이트
//            String name = joinDTO.getName();
//            if (name != null && !name.isBlank()) {
//                user.setName(name);
//            }
//
//            // 이메일 업데이트
//            String email = joinDTO.getEmail();
//            if (email != null && !email.isBlank()) {
//                user.setEmail(email);
//            }

            // 성별 업데이트
            String gender = joinDTO.getGender();
            if (gender != null && !gender.isBlank()) {
                user.setGender(gender);
            }

            // 키 업데이트
            Integer height = joinDTO.getHeight();
            if (height != null) {
                user.setHeight(height);
            }

            // 몸무게 업데이트
            Integer weight = joinDTO.getWeight();
            if (weight != null) {
                user.setWeight(weight);
            }

            userRepository.save(user);
            return user.getUserId();
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            return  -1;
        }
    }
}
