package com.teerthu.expensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // This applies CORS permission to ALL endpoints (/**) in your app
                registry.addMapping("/**")
                        .allowedOrigins("*") // Allows access from any frontend (like Live Server or file://)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP actions
                        .allowedHeaders("*"); // Allowed headers
            }
        };
    }
}