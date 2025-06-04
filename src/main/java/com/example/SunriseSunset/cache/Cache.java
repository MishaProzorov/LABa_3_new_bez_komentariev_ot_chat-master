package com.example.SunriseSunset.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class Cache {

    @Bean
    public Map<String, Object> entityCache() {
        return new HashMap<>();
    }
}