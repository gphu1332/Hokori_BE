package com.hokori.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Configuration to set application default timezone to Vietnam (Asia/Ho_Chi_Minh)
 * This ensures consistent timezone handling across the application
 * 
 * Note: Database timestamps are stored in UTC (Instant), but business logic
 * uses Vietnam timezone for date calculations (e.g., revenue by month)
 */
@Configuration
@Slf4j
public class TimezoneConfig {
    
    private static final String VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";
    
    @PostConstruct
    public void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(VIETNAM_TIMEZONE));
        log.info("✅ Application timezone set to: {} (UTC+7)", VIETNAM_TIMEZONE);
    }
}

