package com.codequest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * WHY a Clock bean: services ask the injected Clock for "now" instead of calling Instant.now().
 * In tests we inject a fixed Clock, so streak/midnight logic can be tested at any date and time.
 */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
