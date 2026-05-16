package ru.practicum.config;

import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignRetryConfig {

    @Value("${feign.retry.period}")
    private long period;

    @Value("${feign.retry.maxPeriod}")
    private long maxPeriod;

    @Value("${feign.retry.maxAttempts}")
    private int maxAttempts;

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(period, maxPeriod, maxAttempts);
    }
}
