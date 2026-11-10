package com.skillforge.authservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Configuration
public class UserServiceCircuitBreakerConfig {

    @Bean
    public CircuitBreaker userServiceCircuitBreaker(
            @Value("${user-service.circuit-breaker.sliding-window-size:10}")
            int slidingWindowSize,
            @Value("${user-service.circuit-breaker.minimum-number-of-calls:5}")
            int minimumNumberOfCalls,
            @Value("${user-service.circuit-breaker.failure-rate-threshold:50}")
            float failureRateThreshold,
            @Value("${user-service.circuit-breaker.wait-duration-in-open-state:10s}")
            Duration waitDurationInOpenState,
            @Value("${user-service.circuit-breaker.permitted-number-of-calls-in-half-open-state:3}")
            int permittedNumberOfCallsInHalfOpenState
    ) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .recordException(UserServiceCircuitBreakerConfig::isDownstreamFailure)
                .build();

        return CircuitBreaker.of("user-service", circuitBreakerConfig);
    }

    private static boolean isDownstreamFailure(Throwable failure) {
        if (failure instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return failure instanceof RestClientException;
    }
}
