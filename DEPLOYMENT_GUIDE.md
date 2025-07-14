# Gainsight PX Integration - Deployment & Operations Guide

## 🎯 Overview

This guide provides step-by-step instructions for deploying and operating the Gainsight PX Integration application in different environments.

## 📋 Pre-Deployment Requirements

### System Requirements
- **Java**: OpenJDK 17 or higher
- **Memory**: Minimum 512MB RAM, Recommended 2GB+
- **Storage**: 1GB+ for application and logs
- **Network**: Outbound HTTPS access to api.aptrinsic.com
- **Database**: MongoDB Atlas (recommended) or local MongoDB installation

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
# MongoDB Configuration (MongoDB Atlas)
spring.data.mongodb.uri=mongodb+srv://\${MONGO_USERNAME:gainsight_user}:\${MONGO_PASSWORD}@\${MONGO_CLUSTER}/gainsightdb?retryWrites=true&w=majority
spring.data.mongodb.database=gainsightdb

# Alternative local MongoDB configuration
# spring.data.mongodb.host=localhost
# spring.data.mongodb.port=27017
# spring.data.mongodb.database=gainsightdb
# spring.data.mongodb.username=gainsight_user
# spring.data.mongodb.password=\${MONGO_PASSWORD}
# spring.data.mongodb.authentication-database=admin

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
Environment=MONGO_USERNAME=gainsight_user
Environment=MONGO_PASSWORD=your_secure_password
Environment=MONGO_CLUSTER=your_atlas_cluster.mongodb.net
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
      - MONGO_USERNAME=gainsight_user
      - MONGO_PASSWORD=${MONGO_PASSWORD}
      - MONGO_CLUSTER=${MONGO_CLUSTER}
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
      - mongodb

  mongodb:
    image: mongo:latest
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD}
      - POSTGRES_USER=gainsight_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - mongodb_data:/data/db
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

### MongoDB Atlas Setup (Recommended)
```bash
# 1. Create MongoDB Atlas account at https://cloud.mongodb.com
# 2. Create a new cluster
# 3. Create a database user:
#    - Username: gainsight_user
#    - Password: your_secure_password
# 4. Whitelist your application's IP addresses
# 5. Get your connection string:
#    mongodb+srv://gainsight_user:your_secure_password@cluster0.xxxxx.mongodb.net/gainsightdb?retryWrites=true&w=majority
```

### Local MongoDB Setup
```bash
# Install MongoDB (macOS with Homebrew)
brew tap mongodb/brew
brew install mongodb-community

# Start MongoDB service
brew services start mongodb/brew/mongodb-community

# Create database and user
mongosh
use gainsightdb
db.createUser({
  user: "gainsight_user",
  pwd: "your_secure_password",
  roles: [
    { role: "readWrite", db: "gainsightdb" }
  ]
})
```

### MongoDB Indexes
```javascript
// Connect to your MongoDB instance
use gainsightdb

// Create indexes for tenant_configurations collection
db.tenant_configurations.createIndex({ "tenantId": 1 }, { unique: true })
db.tenant_configurations.createIndex({ "active": 1 })
db.tenant_configurations.createIndex({ "lastAttemptedExtraction": 1 })

// Create indexes for extracted_events collection
db.extracted_events.createIndex({ "tenantId": 1 })
db.extracted_events.createIndex({ "eventId": 1 })
db.extracted_events.createIndex({ "extractedAt": 1 })
db.extracted_events.createIndex({ "status": 1 })
db.extracted_events.createIndex({ "tenantId": 1, "extractedAt": 1 })
```

## 🔧 Configuration Management

### Environment Variables
```bash
# Required environment variables
export MONGO_USERNAME=gainsight_user
export MONGO_PASSWORD=your_secure_password
export MONGO_CLUSTER=your_atlas_cluster.mongodb.net
export SPRING_PROFILES_ACTIVE=prod

# Optional environment variables
export JAVA_OPTS="-Xmx2g -Xms1g"
export GAINSIGHT_API_TIMEOUT=60
export LOG_LEVEL=INFO
```

### Configuration Profiles

#### Development (application-dev.properties)
```properties
# MongoDB Configuration - Local Development
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=gainsightdb_dev
logging.level.org.example.gainsightapp=DEBUG
```

#### Staging (application-staging.properties)
```properties
# MongoDB Configuration - Atlas Staging
spring.data.mongodb.uri=mongodb+srv://staging_user:password@staging-cluster.mongodb.net/gainsightdb_staging?retryWrites=true&w=majority
spring.data.mongodb.database=gainsightdb_staging
logging.level.org.example.gainsightapp=INFO
```

#### Production (application-prod.properties)
```properties
# MongoDB Configuration - Atlas Production
spring.data.mongodb.uri=mongodb+srv://prod_user:password@prod-cluster.mongodb.net/gainsightdb?retryWrites=true&w=majority
spring.data.mongodb.database=gainsightdb
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
# MongoDB Atlas backup (automated by Atlas)
# Manual backup using mongodump
mongodump --uri="mongodb+srv://username:password@cluster.mongodb.net/gainsightdb" --out="/path/to/backup/$(date +%Y%m%d_%H%M%S)"

# Automated backup script
cat > /opt/gainsight-app/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/gainsight-app/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Database backup
mongodump --uri="$MONGODB_URI" --out="$BACKUP_DIR/db_backup_$DATE"

# Configuration backup
cp -r /opt/gainsight-app/config "$BACKUP_DIR/config_backup_$DATE"

# Clean up old backups (keep last 7 days)
find "$BACKUP_DIR" -name "db_backup_*" -mtime +7 -exec rm -rf {} \;
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

# Database recovery (MongoDB)
mongorestore --uri="$MONGODB_URI" /path/to/backup/directory
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
```javascript
// MongoDB optimization - run in mongosh
use gainsightdb

// Analyze collection statistics
db.tenant_configurations.getIndexes()
db.extracted_events.getIndexes()

// Check collection stats
db.stats()
db.tenant_configurations.stats()
db.extracted_events.stats()

// Optimize queries with explain
db.extracted_events.find({"tenantId": "tenant-001"}).explain("executionStats")
```

### Application Tuning
```properties
# MongoDB connection tuning
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/gainsightdb?maxPoolSize=20&minPoolSize=5&maxIdleTimeMS=30000

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
# Test MongoDB connectivity
mongosh "mongodb+srv://username:password@cluster.mongodb.net/gainsightdb"

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
