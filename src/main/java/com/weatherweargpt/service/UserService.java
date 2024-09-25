package com.weatherweargpt.service;

import com.weatherweargpt.dto.JoinDTO;
import com.weatherweargpt.dto.UserDTO;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
        data.setRole("ROLE_ADMIN");

        userRepository.save(data);
    }

    public void delete(Long Id) {
        userRepository.deleteById(Id);
    }
}
