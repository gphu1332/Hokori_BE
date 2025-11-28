package com.hokori.web.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * DataSource Configuration for Railway PostgreSQL
 * Parses DATABASE_URL from Railway environment variable
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    @Profile("prod")
    public DataSource dataSource() {
        // Check if DATABASE_URL is set (Railway format)
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("postgresql://")) {
            try {
                // Parse Railway DATABASE_URL format: postgresql://user:password@host:port/database
                URI dbUri = new URI(databaseUrl);
                
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                String database = dbUri.getPath().replaceFirst("/", "");
                
                // Build JDBC URL with connection parameters for Railway PostgreSQL
                String jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%d/%s?connectTimeout=60&socketTimeout=60&loginTimeout=60",
                    host, port, database
                );
                
                logger.info("✅ Parsed DATABASE_URL: {}", jdbcUrl.replaceAll(":[^:@]+@", ":****@"));
                logger.info("✅ Database: {}", database);
                logger.info("✅ Host: {}:{}", host, port);
                
                // Configure HikariCP
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                
                // Connection pool settings - increased timeouts for Railway
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(60000); // 60 seconds (increased from 30s)
                config.setValidationTimeout(5000); // 5 seconds
                config.setIdleTimeout(600000); // 10 minutes
                config.setMaxLifetime(1800000); // 30 minutes
                
                // Additional PostgreSQL-specific settings
                config.addDataSourceProperty("socketTimeout", "60");
                config.addDataSourceProperty("loginTimeout", "60");
                config.addDataSourceProperty("tcpKeepAlive", "true");
                
                return new HikariDataSource(config);
            } catch (Exception e) {
                logger.error("❌ Failed to parse DATABASE_URL: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to configure DataSource from DATABASE_URL", e);
            }
        }
        
        // If DATABASE_URL not found, throw exception to prevent startup with invalid config
        throw new IllegalStateException("DATABASE_URL environment variable is required for production profile but was not found or is not in Railway format");
    }
}

