# 🚀 Hokori Web - Setup Guide

## 📋 Prerequisites
- Java 17+
- Maven 3.6+
- SQL Server (localhost:1433)
- Ngrok (for external access)

## 🔧 Setup Steps

### 1. Database Setup
```sql
-- Create database
CREATE DATABASE hokori_db;
```

### 2. Configuration Files
Copy template files and update with your values:

```bash
# Copy template files
cp src/main/resources/application-dev.properties.template src/main/resources/application-dev.properties
cp src/main/resources/google-cloud-service-account.json.template src/main/resources/google-cloud-service-account.json
cp src/main/resources/firebase-service-account.json.template src/main/resources/firebase-service-account.json
```

### 3. Update Configuration
Edit `src/main/resources/application-dev.properties`:
- Update database password
- Update JWT secret (optional)
- Update Google Cloud project ID (optional)

### 4. Start Application
```bash
# Option 1: Use batch file (Windows)
./start-app.bat

# Option 2: Manual start
./mvnw spring-boot:run
```

### 5. Start Ngrok
```bash
ngrok http 8080
```

### 6. Update Swagger URL
After starting ngrok, update the URL in:
- `src/main/java/com/hokori/web/config/SwaggerConfig.java`
- Replace `https://ba0ec26e708c.ngrok-free.app` with your ngrok URL

## 🔗 API Endpoints
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/actuator/health`

## 🚨 Important Notes
- All sensitive files are in `.gitignore`
- Never commit `application-dev.properties` or credential files
- Use template files for setup
- Update ngrok URL when it changes

## 🆘 Troubleshooting
- Port 8080 in use: Kill Java processes first
- Database connection failed: Check SQL Server is running
- JWT errors: Check `application-dev.properties` exists
- AI features disabled: Waiting for Google Cloud credentials
