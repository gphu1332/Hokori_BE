package com.hokori.web.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to fix database constraints on application startup.
 * This ensures database constraints match the current application code.
 */
@Service
public class DatabaseConstraintService {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConstraintService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Fix course_status_check constraint on startup to ensure it allows all CourseStatus values.
     * This is idempotent - safe to run multiple times.
     */
    @PostConstruct
    public void fixCourseStatusConstraint() {
        try {
            log.info("Checking course_status_check constraint...");
            
            // Drop existing constraint if exists
            jdbcTemplate.execute("ALTER TABLE course DROP CONSTRAINT IF EXISTS course_status_check");
            log.info("Dropped existing course_status_check constraint (if existed)");
            
            // Add new constraint with all CourseStatus values including PENDING_UPDATE
            jdbcTemplate.execute(
                "ALTER TABLE course " +
                "ADD CONSTRAINT course_status_check " +
                "CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'PENDING_UPDATE', 'FLAGGED', 'ARCHIVED'))"
            );
            
            log.info("Course status constraint fixed successfully. " +
                    "Constraint now allows: DRAFT, PENDING_APPROVAL, REJECTED, PUBLISHED, PENDING_UPDATE, FLAGGED, ARCHIVED");
        } catch (Exception e) {
            // Log error but don't fail startup - constraint might not exist or might be in different format
            log.warn("Failed to fix course_status_check constraint on startup: {}. " +
                    "You may need to fix it manually via POST /api/admin/database/fix-course-status-constraint", 
                    e.getMessage());
        }
    }
    
    /**
     * Fix sections_study_type_check constraint on startup to ensure it allows QUIZ.
     * This is idempotent - safe to run multiple times.
     */
    @PostConstruct
    public void fixSectionsStudyTypeConstraint() {
        try {
            log.info("Checking sections_study_type_check constraint...");
            
            // Drop existing constraint if exists
            jdbcTemplate.execute("ALTER TABLE sections DROP CONSTRAINT IF EXISTS sections_study_type_check");
            log.info("Dropped existing sections_study_type_check constraint (if existed)");
            
            // Add new constraint with QUIZ included
            jdbcTemplate.execute(
                "ALTER TABLE sections " +
                "ADD CONSTRAINT sections_study_type_check " +
                "CHECK (study_type IN ('GRAMMAR', 'VOCABULARY', 'KANJI', 'QUIZ'))"
            );
            
            log.info("Sections study_type constraint fixed successfully. " +
                    "Constraint now allows: GRAMMAR, VOCABULARY, KANJI, QUIZ");
        } catch (Exception e) {
            // Log error but don't fail startup - constraint might not exist or might be in different format
            log.warn("Failed to fix sections_study_type_check constraint on startup: {}. " +
                    "Migration script will handle it on next deploy", 
                    e.getMessage());
        }
    }
    
    /**
     * Fix sections_content_content_format_check constraint on startup to ensure it allows QUIZ.
     * This is idempotent - safe to run multiple times.
     */
    @PostConstruct
    public void fixSectionsContentFormatConstraint() {
        try {
            log.info("Checking sections_content_content_format_check constraint...");
            
            // Drop existing constraint if exists
            jdbcTemplate.execute("ALTER TABLE sections_content DROP CONSTRAINT IF EXISTS sections_content_content_format_check");
            log.info("Dropped existing sections_content_content_format_check constraint (if existed)");
            
            // Add new constraint with QUIZ included
            jdbcTemplate.execute(
                "ALTER TABLE sections_content " +
                "ADD CONSTRAINT sections_content_content_format_check " +
                "CHECK (content_format IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ'))"
            );
            
            log.info("Sections content_format constraint fixed successfully. " +
                    "Constraint now allows: ASSET, RICH_TEXT, FLASHCARD_SET, QUIZ");
        } catch (Exception e) {
            // Log error but don't fail startup - constraint might not exist or might be in different format
            log.warn("Failed to fix sections_content_content_format_check constraint on startup: {}. " +
                    "Migration script will handle it on next deploy", 
                    e.getMessage());
        }
    }
}

