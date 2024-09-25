package com.weatherweargpt.service;

import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.dto.UserDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
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
            if (!joinDTO.getPassword().isBlank()) {
                user.setPassword(bCryptPasswordEncoder.encode(joinDTO.getPassword()));
            }
            if (!joinDTO.getName().isBlank()) {
                user.setName(joinDTO.getName());
            }
            if (!joinDTO.getGender().isBlank()) {
                user.setGender(joinDTO.getGender());
            }
            if (!joinDTO.getHeight().isBlank()) {
                user.setHeight(joinDTO.getHeight());
            }
            if (!joinDTO.getWeight().isBlank()) {
                user.setWeight(joinDTO.getWeight());
            }
            if (!joinDTO.getEmail().isBlank()) {
                user.setEmail(joinDTO.getEmail());
            }

            userRepository.save(user);

            return user.getId();
        } catch (Exception e) {
            return  -1;
        }
    }
}
