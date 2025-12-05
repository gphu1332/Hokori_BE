package com.hokori.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migration component to allow NULL values in jlpt_test_attempt_answers.selected_option_id
 * This is needed because unanswered questions should have selected_option_id = NULL
 */
@Slf4j
@Component
@DependsOn("dataSource")
@RequiredArgsConstructor
public class JlptTestAttemptAnswerMigration {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Run migration after application is fully ready.
     * Alter table to allow NULL in selected_option_id column.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void migrateSelectedOptionIdNullable() {
        try {
            log.info("Starting migration: Allow NULL in jlpt_test_attempt_answers.selected_option_id");
            
            // Check if column already allows NULL (PostgreSQL)
            String checkSql = """
                SELECT is_nullable 
                FROM information_schema.columns 
                WHERE table_name = 'jlpt_test_attempt_answers' 
                AND column_name = 'selected_option_id'
                """;
            
            try {
                String isNullable = jdbcTemplate.queryForObject(checkSql, String.class);
                
                if ("YES".equalsIgnoreCase(isNullable)) {
                    log.info("✅ Migration skipped: selected_option_id already allows NULL");
                    return;
                }
            } catch (Exception e) {
                // Table or column might not exist yet, continue with migration
                log.debug("Could not check column status: {}", e.getMessage());
            }
            
            // Alter table to allow NULL
            String alterSql = """
                ALTER TABLE jlpt_test_attempt_answers 
                ALTER COLUMN selected_option_id DROP NOT NULL
                """;
            
            jdbcTemplate.execute(alterSql);
            log.info("✅ Migration completed: selected_option_id now allows NULL values");
            
        } catch (Exception e) {
            // Check if error is because constraint doesn't exist or column already nullable
            String errorMsg = e.getMessage();
            if (errorMsg != null && (
                errorMsg.contains("does not exist") || 
                errorMsg.contains("already") ||
                errorMsg.contains("is not null")
            )) {
                log.info("✅ Migration skipped: Column already allows NULL or constraint doesn't exist");
            } else {
                log.error("❌ Migration failed: {}", e.getMessage(), e);
                // Don't throw exception to allow application to start
                // Migration will be retried on next startup
            }
        }
    }
}

