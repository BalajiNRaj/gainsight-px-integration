# Gainsight PX Integration - Quick Reference Card

## 🚀 Quick Start Commands

```bash
# Start application
mvn spring-boot:run

# Run tests
mvn test

# Build for production
mvn clean package
```

## 🔗 Key URLs
- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/api/monitoring/health
- **All Tenants**: http://localhost:8080/api/tenants

## 📋 Essential API Commands

### Tenant Management
```bash
# List all tenants
curl http://localhost:8080/api/tenants

# Get specific tenant
curl http://localhost:8080/api/tenants/tenant-001

# Create new tenant
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"new-tenant","companyName":"New Corp","apiKey":"your-key"}'

# Test connection
curl -X POST http://localhost:8080/api/tenants/tenant-001/test-connection

# Activate tenant
curl -X POST http://localhost:8080/api/tenants/tenant-001/activate

# Trigger extraction
curl -X POST http://localhost:8080/api/tenants/tenant-001/extract

# Get tenant stats
curl http://localhost:8080/api/tenants/tenant-001/stats
```

### Monitoring
```bash
# Health check
curl http://localhost:8080/api/monitoring/health

# System metrics
curl http://localhost:8080/api/monitoring/metrics

# System status
curl http://localhost:8080/api/monitoring/status
```

## 🗄️ Database Quick Access

**MongoDB Atlas Dashboard**: Access through your MongoDB Atlas account
- **Database**: gainsightdb
- **Collections**: tenant_configurations, extracted_events

### Useful MongoDB Queries
```javascript
// View all tenants
db.tenant_configurations.find()

// View recent events
db.extracted_events.find().sort({extractedAt: -1}).limit(10)

// Check extraction stats by tenant
db.extracted_events.aggregate([
  {$group: {
    _id: "$tenantId", 
    events: {$sum: 1}, 
    lastExtraction: {$max: "$extractedAt"}
  }}
])

// View failed extractions
db.extracted_events.find({status: "FAILED"})
```

## 🗄️ MongoDB Atlas Setup

### Quick Atlas Setup
```bash
# 1. Sign up at https://cloud.mongodb.com
# 2. Create cluster (Free tier: M0 Sandbox)
# 3. Create database user:
#    - Username: gainsight_user
#    - Password: [generate secure password]
# 4. Network Access: Add IP addresses or 0.0.0.0/0 (less secure)
# 5. Get connection string from "Connect" button
```

### Connection String Examples
```properties
# MongoDB Atlas (recommended)
spring.data.mongodb.uri=mongodb+srv://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@cluster0.xxxxx.mongodb.net/gainsightdb?retryWrites=true&w=majority

# Local MongoDB
spring.data.mongodb.uri=mongodb://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@localhost:27017/gainsightdb?authSource=admin
```

### Essential MongoDB Commands
```javascript
// Connect to MongoDB
mongosh "your_connection_string"

// Basic operations
use gainsightdb
show collections
db.tenant_configurations.countDocuments()
db.extracted_events.countDocuments()

// View recent events
db.extracted_events.find().sort({extractedAt: -1}).limit(5)

// Find failed extractions
db.extracted_events.find({status: "FAILED"})

// Check tenant status
db.tenant_configurations.find({active: true})

// Clean old events (older than 30 days)
db.extracted_events.deleteMany({
  extractedAt: {
    $lt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  }
})
```

### MongoDB Performance Tips
```javascript
// Create essential indexes
db.tenant_configurations.createIndex({tenantId: 1}, {unique: true})
db.extracted_events.createIndex({tenantId: 1, extractedAt: 1})
db.extracted_events.createIndex({status: 1})

// Check query performance
db.extracted_events.find({tenantId: "tenant-001"}).explain("executionStats")

// Monitor collection stats
db.stats()
db.extracted_events.stats()
```

## ⚙️ Configuration Files

### Main Config: `application.properties`
```properties
# MongoDB
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/gainsightdb

# Gainsight PX
gainsight.px.default.api-url=https://api.aptrinsic.com
gainsight.px.default.timeout-seconds=30
gainsight.px.default.max-retry-attempts=3

# Logging
logging.level.org.example.gainsightapp=INFO
```

## 🔧 Troubleshooting Quick Fixes

### Application Won't Start
```bash
# Check if port 8080 is available
lsof -i :8080

# Kill process using port 8080
kill -9 $(lsof -t -i:8080)
```

### Check Logs
```bash
# Real-time log monitoring
tail -f logs/application.log

# Filter for errors
grep -E "(ERROR|WARN)" logs/application.log

# Check extraction activity
grep "extraction" logs/application.log
```

### Reset Data
```bash
# Connect to MongoDB and drop collections
# Use MongoDB Compass or mongo shell:
# db.tenant_configurations.drop()
# db.extracted_events.drop()
```

## 📊 Monitoring Commands

### Check System Health
```bash
# Quick health check
curl -s http://localhost:8080/api/monitoring/health | jq .status

# Get active tenant count
curl -s http://localhost:8080/api/monitoring/metrics | jq .tenants.active

# Check for tenants with errors
curl -s http://localhost:8080/api/monitoring/health | jq .tenantsWithErrors
```

### Performance Monitoring
```bash
# Check extraction activity for all tenants
for tenant in $(curl -s http://localhost:8080/api/tenants | jq -r '.[].tenantId'); do
  echo "=== $tenant ==="
  curl -s http://localhost:8080/api/tenants/$tenant/stats | jq .
done
```

## 🔑 Sample Tenant Configuration

```json
{
  "tenantId": "example-corp",
  "companyName": "Example Corporation", 
  "apiKey": "your-gainsight-px-api-key",
  "apiUrl": "https://api.aptrinsic.com",
  "active": true,
  "extractionIntervalMinutes": 5,
  "extractCustomEvents": true,
  "extractStandardEvents": true,
  "maxRetryAttempts": 3,
  "timeoutSeconds": 30
}
```

## 🚨 Emergency Procedures

### Stop All Extractions
```bash
# Deactivate all tenants
for tenant in $(curl -s http://localhost:8080/api/tenants | jq -r '.[].tenantId'); do
  curl -X POST http://localhost:8080/api/tenants/$tenant/deactivate
done
```

### Check System Resources
```bash
# Memory usage
ps aux | grep java

# Disk usage
df -h

# Network connections
netstat -an | grep 8080
```

## 📞 Support Information

- **Log Location**: `logs/application.log`
- **Config Location**: `src/main/resources/application.properties`
- **Main Application Class**: `org.example.gainsightapp.app.GainsightAppApplication`
- **Database**: MongoDB Atlas (cloud) or local MongoDB instance
- **Default Port**: 8080

---
**Quick Reference Version**: 1.0.0 | **Last Updated**: July 13, 2025
