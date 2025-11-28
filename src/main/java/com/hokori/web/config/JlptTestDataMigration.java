package com.hokori.web.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Data migration component to fix NULL values in jlpt_tests.current_participants
 * This runs once on startup in production to fix existing NULL values
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class JlptTestDataMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateCurrentParticipants() {
        try {
            log.info("Starting migration: Fix NULL values in jlpt_tests.current_participants");
            
            int updated = jdbcTemplate.update(
                "UPDATE jlpt_tests SET current_participants = 0 WHERE current_participants IS NULL"
            );
            
            if (updated > 0) {
                log.info("✅ Migration completed: Updated {} rows with NULL current_participants to 0", updated);
            } else {
                log.info("✅ Migration completed: No NULL values found in current_participants");
            }
        } catch (Exception e) {
            log.error("❌ Migration failed: {}", e.getMessage(), e);
            // Don't throw exception to allow application to start
            // Migration will be retried on next startup
        }
    }
}

