package com.hokori.web.util;

/**
 * Utility class for database-related operations.
 * Helps detect database type for compatibility between SQL Server and PostgreSQL.
 */
public class DatabaseUtil {
    
    /**
     * Detect if current database is PostgreSQL.
     * SQL Server doesn't have LOB stream issues, so we can use entity directly.
     * 
     * @return true if PostgreSQL, false if SQL Server (default)
     */
    public static boolean isPostgreSQLDatabase() {
        try {
            // Check active profile first (most reliable)
            String activeProfile = System.getProperty("spring.profiles.active", 
                    System.getenv("SPRING_PROFILES_ACTIVE"));
            if ("prod".equals(activeProfile)) {
                return true; // Production uses PostgreSQL on Railway
            }
            
            // Check DATABASE_URL (Railway PostgreSQL)
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null && (databaseUrl.contains("postgresql://") || databaseUrl.contains("postgres://"))) {
                return true;
            }
            
            // Check JDBC URL from system properties
            String datasourceUrl = System.getProperty("spring.datasource.url");
            if (datasourceUrl != null) {
                return datasourceUrl.contains("postgresql") || datasourceUrl.contains("postgres");
            }
            
            // Default: assume dev (SQL Server) unless explicitly PostgreSQL
            return false;
        } catch (Exception e) {
            // If detection fails, default to false (SQL Server) for safety
            return false;
        }
    }
    
    /**
     * Check if we should use native queries to avoid LOB stream errors.
     * Only needed for PostgreSQL.
     */
    public static boolean shouldUseNativeQueryForLOB() {
        return isPostgreSQLDatabase();
    }
}

