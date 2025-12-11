package com.hokori.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Data migration component to set isTrial = true for the first chapter (orderIndex = 0)
 * of existing courses that don't have a trial chapter yet.
 * This ensures backward compatibility with old courses.
 * 
 * This runs after application is fully started to ensure DataSource is ready.
 */
@Slf4j
@Component
@Profile("prod")
@DependsOn("dataSource")
@RequiredArgsConstructor
public class ChapterTrialMigration {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Run migration after application is fully ready (not during startup)
     * This ensures DataSource is fully initialized before attempting migration
     */
    @EventListener(ApplicationReadyEvent.class)
    public void migrateFirstChaptersToTrial() {
        try {
            log.info("Starting migration: Set isTrial = true for first chapters of existing courses");
            
            // Update chapters where orderIndex = 0 and isTrial = false
            // Only update if the course doesn't already have a trial chapter
            String sql = """
                UPDATE chapter c1
                SET is_trial = true
                WHERE c1.order_index = 0
                  AND c1.is_trial = false
                  AND NOT EXISTS (
                      SELECT 1 
                      FROM chapter c2 
                      WHERE c2.course_id = c1.course_id 
                        AND c2.is_trial = true
                        AND c2.id != c1.id
                  )
                """;
            
            int updated = jdbcTemplate.update(sql);
            
            if (updated > 0) {
                log.info("✅ Migration completed: Set isTrial = true for {} first chapters", updated);
            } else {
                log.info("✅ Migration completed: No chapters needed to be updated (all courses already have trial chapters or no first chapters found)");
            }
        } catch (Exception e) {
            log.error("❌ Migration failed: {}", e.getMessage(), e);
            // Don't throw exception to allow application to start
            // Migration will be retried on next startup
        }
    }
}

