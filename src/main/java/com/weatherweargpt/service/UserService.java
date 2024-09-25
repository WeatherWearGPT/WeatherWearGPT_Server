package com.weatherweargpt.service;

import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.dto.UserDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void join(JoinDTO joinDTO) {
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();
        String name = joinDTO.getName();
        String email = joinDTO.getEmail();
        String gender = joinDTO.getGender();
        String height = joinDTO.getHeight();
        String weight = joinDTO.getWeight();

        UserEntity data = new UserEntity();
        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
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
                .orElseThrow(() -> new RuntimeException("User not found")); // 예외 발생

        JoinDTO joinDTO = new JoinDTO();
        joinDTO.setUsername(user.getUsername());
        joinDTO.setName(user.getName());
        joinDTO.setHeight(user.getHeight());
        joinDTO.setWeight(user.getWeight());
        joinDTO.setGender(user.getGender());
        joinDTO.setEmail(user.getEmail());
        return joinDTO;
    }

    public long updateUser(UserEntity user, JoinDTO joinDTO) {
        try {
            // 비밀번호 업데이트
            String password = joinDTO.getPassword();
            if (password != null && !password.isBlank()) {
                user.setPassword(bCryptPasswordEncoder.encode(password));
            }

            // 이름 업데이트
            String name = joinDTO.getName();
            if (name != null && !name.isBlank()) {
                user.setName(name);
            }

            // 성별 업데이트
            String gender = joinDTO.getGender();
            if (gender != null && !gender.isBlank()) {
                user.setGender(gender);
            }

            // 키 업데이트
            String height = joinDTO.getHeight();
            if (height != null && !height.isBlank()) {
                user.setHeight(height);
            }

            // 몸무게 업데이트
            String weight = joinDTO.getWeight();
            if (weight != null && !weight.isBlank()) {
                user.setWeight(weight);
            }

            // 이메일 업데이트
            String email = joinDTO.getEmail();
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            }
            userRepository.save(user);
            return user.getId();
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            return  -1;
        }
    }
}
