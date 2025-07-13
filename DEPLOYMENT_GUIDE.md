# Gainsight PX Integration - Deployment & Operations Guide

## 🎯 Overview

This guide provides step-by-step instructions for deploying and operating the Gainsight PX Integration application in different environments.

## 📋 Pre-Deployment Requirements

### System Requirements
- **Java**: OpenJDK 17 or higher
- **Memory**: Minimum 512MB RAM, Recommended 2GB+
- **Storage**: 1GB+ for application and logs
- **Network**: Outbound HTTPS access to api.aptrinsic.com
- **Database**: H2 (development) or PostgreSQL/MySQL (production)

### Dependencies
- Maven 3.6+
- Git (for source code)
- Docker (optional, for containerized deployment)

## 🚀 Deployment Options

### Option 1: Traditional JAR Deployment

#### 1. Build Application
```bash
# Clone repository
git clone <repository-url>
cd GainsightApp

# Build application
mvn clean package -DskipTests

# Verify JAR creation
ls -la target/GainsightApp-*.jar
```

#### 2. Prepare Environment
```bash
# Create application directory
sudo mkdir -p /opt/gainsight-app
sudo mkdir -p /opt/gainsight-app/logs
sudo mkdir -p /opt/gainsight-app/config

# Copy JAR file
sudo cp target/GainsightApp-*.jar /opt/gainsight-app/gainsight-app.jar

# Set permissions
sudo chown -R gainsight:gainsight /opt/gainsight-app
```

#### 3. Configuration
```bash
# Create production configuration
sudo tee /opt/gainsight-app/config/application-prod.properties << EOF
# Database Configuration (PostgreSQL example)
spring.datasource.url=jdbc:postgresql://localhost:5432/gainsightdb
spring.datasource.username=\${DB_USERNAME:gainsight_user}
spring.datasource.password=\${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Security
spring.h2.console.enabled=false

# Gainsight PX Configuration
gainsight.px.default.api-url=https://api.aptrinsic.com
gainsight.px.default.timeout-seconds=30
gainsight.px.default.max-retry-attempts=3

# Logging
logging.level.org.example.gainsightapp=INFO
logging.file.name=/opt/gainsight-app/logs/application.log
logging.file.max-size=100MB
logging.file.max-history=30

# Server Configuration
server.port=8080
management.endpoints.web.exposure.include=health,metrics,info
EOF
```

#### 4. Systemd Service (Linux)
```bash
# Create systemd service file
sudo tee /etc/systemd/system/gainsight-app.service << EOF
[Unit]
Description=Gainsight PX Integration Application
After=network.target

[Service]
Type=simple
User=gainsight
Group=gainsight
WorkingDirectory=/opt/gainsight-app
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/gainsight-app/gainsight-app.jar
Restart=always
RestartSec=30
StandardOutput=journal
StandardError=journal
SyslogIdentifier=gainsight-app

# Environment variables
Environment=DB_USERNAME=gainsight_user
Environment=DB_PASSWORD=your_secure_password
Environment=JAVA_OPTS=-Xmx1g -Xms512m

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and start service
sudo systemctl daemon-reload
sudo systemctl enable gainsight-app
sudo systemctl start gainsight-app

# Check status
sudo systemctl status gainsight-app
```

### Option 2: Docker Deployment

#### 1. Create Dockerfile
```dockerfile
FROM openjdk:17-jre-slim

# Create app directory
RUN mkdir -p /app/logs /app/config
WORKDIR /app

# Copy JAR file
COPY target/GainsightApp-*.jar app.jar

# Create non-root user
RUN groupadd -r gainsight && useradd -r -g gainsight gainsight
RUN chown -R gainsight:gainsight /app

USER gainsight

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/monitoring/health || exit 1

# Expose port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Build and Run Container
```bash
# Build Docker image
docker build -t gainsight-app:latest .

# Run container with environment variables
docker run -d \
  --name gainsight-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_USERNAME=gainsight_user \
  -e DB_PASSWORD=your_secure_password \
  -v /opt/gainsight-app/logs:/app/logs \
  --restart unless-stopped \
  gainsight-app:latest

# Check container status
docker ps
docker logs gainsight-app
```

#### 3. Docker Compose (Recommended)
```yaml
# docker-compose.yml
version: '3.8'

services:
  gainsight-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=gainsight_user
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/monitoring/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    depends_on:
      - postgres

  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=gainsightdb
      - POSTGRES_USER=gainsight_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

```bash
# Deploy with Docker Compose
echo "DB_PASSWORD=your_secure_password" > .env
docker-compose up -d

# Check services
docker-compose ps
docker-compose logs -f gainsight-app
```

## 🗄️ Database Setup

### PostgreSQL Setup
```sql
-- Create database and user
CREATE DATABASE gainsightdb;
CREATE USER gainsight_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE gainsightdb TO gainsight_user;

-- Connect to gainsightdb and grant schema permissions
\c gainsightdb
GRANT ALL ON SCHEMA public TO gainsight_user;
GRANT ALL ON ALL TABLES IN SCHEMA public TO gainsight_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO gainsight_user;
```

### MySQL Setup
```sql
-- Create database and user
CREATE DATABASE gainsightdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'gainsight_user'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON gainsightdb.* TO 'gainsight_user'@'%';
FLUSH PRIVILEGES;
```

### Schema Migration
```bash
# Initial schema creation (first deployment)
# Set spring.jpa.hibernate.ddl-auto=create-drop for first run
# Then change to spring.jpa.hibernate.ddl-auto=validate

# For subsequent deployments, use migration scripts
# Example migration script:
cat > db/migration/V1_1__add_index.sql << EOF
CREATE INDEX idx_tenant_id ON extracted_events(tenant_id);
CREATE INDEX idx_extracted_at ON extracted_events(extracted_at);
CREATE INDEX idx_status ON extracted_events(status);
EOF
```

## 🔧 Configuration Management

### Environment Variables
```bash
# Required environment variables
export DB_USERNAME=gainsight_user
export DB_PASSWORD=your_secure_password
export SPRING_PROFILES_ACTIVE=prod

# Optional environment variables
export JAVA_OPTS="-Xmx2g -Xms1g"
export GAINSIGHT_API_TIMEOUT=60
export LOG_LEVEL=INFO
```

### Configuration Profiles

#### Development (application-dev.properties)
```properties
spring.datasource.url=jdbc:h2:mem:gainsightdb
spring.jpa.show-sql=true
spring.h2.console.enabled=true
logging.level.org.example.gainsightapp=DEBUG
```

#### Staging (application-staging.properties)
```properties
spring.datasource.url=jdbc:postgresql://staging-db:5432/gainsightdb
spring.jpa.hibernate.ddl-auto=validate
logging.level.org.example.gainsightapp=INFO
```

#### Production (application-prod.properties)
```properties
spring.datasource.url=jdbc:postgresql://prod-db:5432/gainsightdb
spring.jpa.hibernate.ddl-auto=validate
spring.h2.console.enabled=false
logging.level.org.example.gainsightapp=WARN
```

## 📊 Monitoring & Alerting

### Health Check Endpoints
```bash
# Application health
curl http://localhost:8080/api/monitoring/health

# Detailed status
curl http://localhost:8080/api/monitoring/status

# Metrics
curl http://localhost:8080/api/monitoring/metrics
```

### Log Monitoring
```bash
# Monitor application logs
tail -f /opt/gainsight-app/logs/application.log

# Error monitoring
grep -E "(ERROR|WARN)" /opt/gainsight-app/logs/application.log

# Extraction monitoring
grep "extraction" /opt/gainsight-app/logs/application.log | tail -20
```

### Alerting Setup (Example with cron)
```bash
# Create monitoring script
cat > /opt/gainsight-app/monitor.sh << 'EOF'
#!/bin/bash

HEALTH_URL="http://localhost:8080/api/monitoring/health"
ALERT_EMAIL="admin@company.com"

# Check application health
if ! curl -f "$HEALTH_URL" > /dev/null 2>&1; then
    echo "Gainsight App is DOWN!" | mail -s "ALERT: Gainsight App Down" "$ALERT_EMAIL"
fi

# Check for recent errors
ERROR_COUNT=$(grep -c "ERROR" /opt/gainsight-app/logs/application.log | tail -100)
if [ "$ERROR_COUNT" -gt 10 ]; then
    echo "High error count: $ERROR_COUNT errors in last 100 log lines" | \
        mail -s "ALERT: High Error Rate" "$ALERT_EMAIL"
fi
EOF

chmod +x /opt/gainsight-app/monitor.sh

# Add to crontab (run every 5 minutes)
echo "*/5 * * * * /opt/gainsight-app/monitor.sh" | crontab -
```

## 🔄 Backup & Recovery

### Database Backup
```bash
# PostgreSQL backup
pg_dump -h localhost -U gainsight_user gainsightdb > backup_$(date +%Y%m%d_%H%M%S).sql

# Automated backup script
cat > /opt/gainsight-app/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/gainsight-app/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Database backup
pg_dump -h localhost -U gainsight_user gainsightdb > "$BACKUP_DIR/db_backup_$DATE.sql"

# Configuration backup
cp -r /opt/gainsight-app/config "$BACKUP_DIR/config_backup_$DATE"

# Clean up old backups (keep last 7 days)
find "$BACKUP_DIR" -name "*.sql" -mtime +7 -delete
find "$BACKUP_DIR" -name "config_backup_*" -mtime +7 -exec rm -rf {} \;
EOF

chmod +x /opt/gainsight-app/backup.sh

# Schedule daily backups
echo "0 2 * * * /opt/gainsight-app/backup.sh" | crontab -
```

### Recovery Procedures
```bash
# Application recovery
sudo systemctl stop gainsight-app
sudo systemctl start gainsight-app

# Database recovery (PostgreSQL)
sudo systemctl stop gainsight-app
dropdb gainsightdb
createdb gainsightdb
psql gainsightdb < backup_YYYYMMDD_HHMMSS.sql
sudo systemctl start gainsight-app
```

## 🔒 Security Considerations

### SSL/TLS Configuration
```properties
# Enable HTTPS (production)
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEY_STORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=gainsight-app
```

### API Key Management
```bash
# Store API keys securely (example with environment variables)
export GAINSIGHT_API_KEY_TENANT1="encrypted_or_secure_storage"

# Or use external secret management
# - AWS Secrets Manager
# - HashiCorp Vault
# - Kubernetes Secrets
```

### Network Security
```bash
# Firewall rules (iptables example)
# Allow incoming on port 8080 from specific IPs only
iptables -A INPUT -p tcp -s 10.0.0.0/8 --dport 8080 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j DROP

# Allow outgoing HTTPS to Gainsight API
iptables -A OUTPUT -p tcp --dport 443 -d api.aptrinsic.com -j ACCEPT
```

## 📈 Performance Tuning

### JVM Tuning
```bash
# Production JVM settings
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+PrintGCDetails"
```

### Database Tuning
```sql
-- PostgreSQL optimization
CREATE INDEX CONCURRENTLY idx_tenant_extracted_at ON extracted_events(tenant_id, extracted_at);
CREATE INDEX CONCURRENTLY idx_status_retry ON extracted_events(status, retry_count);

-- Analyze tables periodically
ANALYZE tenant_configurations;
ANALYZE extracted_events;
```

### Application Tuning
```properties
# Connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# HTTP client tuning
gainsight.px.default.timeout-seconds=60
gainsight.px.default.max-retry-attempts=3
```

## 🔧 Maintenance Procedures

### Regular Maintenance Tasks
```bash
# Weekly maintenance script
cat > /opt/gainsight-app/maintenance.sh << 'EOF'
#!/bin/bash

echo "Starting weekly maintenance..."

# 1. Log rotation
find /opt/gainsight-app/logs -name "*.log" -size +100M -exec logrotate {} \;

# 2. Database cleanup (remove events older than 90 days)
psql -U gainsight_user -d gainsightdb -c "DELETE FROM extracted_events WHERE extracted_at < NOW() - INTERVAL '90 days';"

# 3. Update statistics
psql -U gainsight_user -d gainsightdb -c "ANALYZE;"

# 4. Check disk space
df -h /opt/gainsight-app

echo "Weekly maintenance completed."
EOF

chmod +x /opt/gainsight-app/maintenance.sh

# Schedule weekly (Sundays at 2 AM)
echo "0 2 * * 0 /opt/gainsight-app/maintenance.sh" | crontab -
```

### Update Procedures
```bash
# Application update procedure
sudo systemctl stop gainsight-app

# Backup current version
cp /opt/gainsight-app/gainsight-app.jar /opt/gainsight-app/gainsight-app.jar.backup

# Deploy new version
cp target/GainsightApp-new-version.jar /opt/gainsight-app/gainsight-app.jar

# Test configuration
java -jar /opt/gainsight-app/gainsight-app.jar --spring.profiles.active=prod --validate-only

# Start service
sudo systemctl start gainsight-app

# Verify deployment
curl -f http://localhost:8080/api/monitoring/health
```

## 🆘 Troubleshooting Guide

### Common Issues

#### Application Won't Start
```bash
# Check Java version
java -version

# Check port availability
netstat -tulpn | grep 8080

# Check logs
journalctl -u gainsight-app -f

# Check configuration
java -jar gainsight-app.jar --spring.profiles.active=prod --debug
```

#### Database Connection Issues
```bash
# Test database connectivity
psql -h localhost -U gainsight_user -d gainsightdb -c "SELECT 1;"

# Check connection pool
curl http://localhost:8080/api/monitoring/status | jq .database
```

#### High Memory Usage
```bash
# Check JVM memory
jstat -gc <pid>

# Generate heap dump
jmap -dump:format=b,file=heapdump.hprof <pid>

# Analyze with tools like Eclipse MAT
```

### Emergency Procedures
```bash
# Emergency shutdown
sudo systemctl stop gainsight-app

# Force kill if needed
sudo pkill -f gainsight-app

# Quick restart
sudo systemctl restart gainsight-app

# Rollback to previous version
cp /opt/gainsight-app/gainsight-app.jar.backup /opt/gainsight-app/gainsight-app.jar
sudo systemctl restart gainsight-app
```

---

## 📞 Support Contacts

- **Application Logs**: `/opt/gainsight-app/logs/application.log`
- **System Logs**: `journalctl -u gainsight-app`
- **Health Check**: `http://localhost:8080/api/monitoring/health`
- **Admin Email**: `admin@company.com`

**Deployment Guide Version**: 1.0.0 | **Last Updated**: July 13, 2025
