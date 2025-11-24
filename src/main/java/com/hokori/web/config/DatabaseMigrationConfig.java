package com.hokori.web.config;

import com.hokori.web.util.DatabaseUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Database migration configuration to fix data issues.
 * Works on both PostgreSQL (Railway) and SQL Server (local SSMS).
 * Runs BEFORE Hibernate schema update to fix NULL values.
 */
@Slf4j
@Configuration
@DependsOn("dataSource") // Ensure DataSource is ready
@Order(1) // Run early, before EntityManagerFactory
public class DatabaseMigrationConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    @Transactional
    public void migrateDatabase() {
        String dbType = DatabaseUtil.isPostgreSQLDatabase() ? "PostgreSQL" : "SQL Server";
        
        try {
            log.info("🔄 Running database migrations on {} (before Hibernate schema update)...", dbType);
            
            // Create JdbcTemplate from DataSource directly
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // Fix NULL values in jlpt_tests.current_participants
            // SQL query is compatible with both PostgreSQL and SQL Server
            fixJlptTestCurrentParticipants(jdbcTemplate);
            
            log.info("✅ Database migrations completed successfully");
        } catch (Exception e) {
            log.error("❌ Error running database migrations: {}", e.getMessage(), e);
            // Don't throw exception to allow app to start even if migration fails
            // The error will be logged for manual investigation
        }
    }

    private void fixJlptTestCurrentParticipants(JdbcTemplate jdbcTemplate) {
        try {
            // Check if column exists and has NULL values
            String checkSql = "SELECT COUNT(*) FROM jlpt_tests WHERE current_participants IS NULL";
            Integer nullCount = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (nullCount != null && nullCount > 0) {
                log.info("🔧 Found {} records with NULL current_participants, updating to 0...", nullCount);
                
                String updateSql = "UPDATE jlpt_tests SET current_participants = 0 WHERE current_participants IS NULL";
                int updated = jdbcTemplate.update(updateSql);
                
                log.info("✅ Updated {} records in jlpt_tests.current_participants", updated);
            } else {
                log.info("✅ No NULL values found in jlpt_tests.current_participants");
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not fix jlpt_tests.current_participants (table might not exist yet): {}", e.getMessage());
        }
    }
}

