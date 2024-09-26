package com.weatherweargpt.config;

import com.weatherweargpt.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        corsRegistry.addMapping("/**")
                .exposedHeaders("Set-Cookie")
                .allowedOrigins("http://localhost:3000");
    }

    @Configuration
    @EnableWebSecurity
    public class SecurityConfig {
        private final CustomOAuth2UserService customOAuth2UserService;

            this.customOAuth2UserService = customOAuth2UserService;
        }
}