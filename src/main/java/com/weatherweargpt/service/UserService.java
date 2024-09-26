package com.weatherweargpt.service;

import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
        joinDTO.setUserName(user.getUserName());
        joinDTO.setName(user.getName());
        joinDTO.setHeight(user.getHeight());
        joinDTO.setWeight(user.getWeight());
        joinDTO.setGender(user.getGender());
        joinDTO.setEmail(user.getEmail());
        return joinDTO;
    }
}