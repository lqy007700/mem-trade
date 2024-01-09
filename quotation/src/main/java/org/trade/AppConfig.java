package org.trade;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class AppConfig {
    @Bean
    public ZoneId zoneId() {
        return ZoneId.systemDefault(); // 或者根据您的需求返回特定的 ZoneId
    }
}