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
- **Database Console**: http://localhost:8080/h2-console
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

**H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:gainsightdb`
- **Username**: `sa`
- **Password**: (empty)

### Useful SQL Queries
```sql
-- View all tenants
SELECT * FROM tenant_configurations;

-- View recent events
SELECT * FROM extracted_events ORDER BY extracted_at DESC LIMIT 10;

-- Check extraction stats by tenant
SELECT tenant_id, COUNT(*) as events, MAX(extracted_at) as last_extraction 
FROM extracted_events GROUP BY tenant_id;

-- View failed extractions
SELECT * FROM extracted_events WHERE status = 'FAILED';
```

## ⚙️ Configuration Files

### Main Config: `application.properties`
```properties
# Database
spring.datasource.url=jdbc:h2:mem:gainsightdb

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
# Restart application (H2 is in-memory, so data resets)
# Or access H2 console and run:
# DROP ALL OBJECTS;
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
- **Database**: H2 (in-memory for development)
- **Default Port**: 8080

---
**Quick Reference Version**: 1.0.0 | **Last Updated**: July 13, 2025
