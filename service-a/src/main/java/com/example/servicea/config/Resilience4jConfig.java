package com.example.servicea.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open after 50% of calls fail
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Consider calls taking > 2s as slow
                .slowCallRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Keep the circuit open for 10s
                .permittedNumberOfCallsInHalfOpenState(10) // Allow 10 calls in half-open state
                .minimumNumberOfCalls(10) // Don't start evaluating until 10 calls have been made
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}