package com.weatherweargpt.service;

import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);

        UserEntity user = userRepository.findByUserName(username);
        if (user == null) {
            logger.error("User not found for username: {}", username);
            throw new UsernameNotFoundException("User not found for username: " + username);
        }

        logger.info("User found: {}", user.getUserName());
        return new CustomUserDetails(user);
    }
}