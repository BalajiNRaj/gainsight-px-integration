# Gainsight PX Integration - Usage Guide & Reference

## Table of Contents
1. [Quick Start](#quick-start)
2. [API Reference](#api-reference)
3. [Usage Examples](#usage-examples)
4. [Configuration Guide](#configuration-guide)
5. [Monitoring & Troubleshooting](#monitoring--troubleshooting)
6. [Important Notes](#important-notes)
7. [Architecture Overview](#architecture-overview)
8. [Development Guide](#development-guide)
9. [Production Deployment](#production-deployment)
10. [FAQ & Common Issues](#faq--common-issues)

## Quick Start

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Access Key Endpoints
- **Application**: http://localhost:8080
- **H2 Database Console**: http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/api/monitoring/health
- **All Tenants**: http://localhost:8080/api/tenants

### 3. Sample Data
The application automatically creates 3 sample tenants on startup:
- `tenant-001`: Acme Corporation (Active)
- `tenant-002`: TechStart Inc (Inactive)
- `tenant-003`: Global Enterprise Ltd (Active)

## API Reference

### Tenant Management Endpoints

#### GET /api/tenants
**Description**: Retrieve all tenant configurations
```bash
curl -X GET http://localhost:8080/api/tenants
```

#### GET /api/tenants/{tenantId}
**Description**: Get specific tenant by ID
```bash
curl -X GET http://localhost:8080/api/tenants/tenant-001
```

#### POST /api/tenants
**Description**: Create new tenant configuration
```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant-004",
    "companyName": "New Company Inc",
    "apiKey": "your-gainsight-api-key",
    "apiUrl": "https://api.aptrinsic.com",
    "active": true,
    "extractionIntervalMinutes": 5,
    "extractCustomEvents": true,
    "extractStandardEvents": true,
    "maxRetryAttempts": 3,
    "timeoutSeconds": 30
  }'
```

#### PUT /api/tenants/{tenantId}
**Description**: Update existing tenant configuration
```bash
curl -X PUT http://localhost:8080/api/tenants/tenant-004 \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant-004",
    "companyName": "Updated Company Name",
    "apiKey": "updated-api-key",
    "extractionIntervalMinutes": 10
  }'
```

#### DELETE /api/tenants/{tenantId}
**Description**: Delete tenant configuration
```bash
curl -X DELETE http://localhost:8080/api/tenants/tenant-004
```

#### POST /api/tenants/{tenantId}/activate
**Description**: Activate tenant for event extraction
```bash
curl -X POST http://localhost:8080/api/tenants/tenant-004/activate
```

#### POST /api/tenants/{tenantId}/deactivate
**Description**: Deactivate tenant (stops event extraction)
```bash
curl -X POST http://localhost:8080/api/tenants/tenant-004/deactivate
```

#### POST /api/tenants/{tenantId}/extract
**Description**: Trigger manual event extraction
```bash
curl -X POST http://localhost:8080/api/tenants/tenant-001/extract
```

#### GET /api/tenants/{tenantId}/stats
**Description**: Get tenant extraction statistics
```bash
curl -X GET http://localhost:8080/api/tenants/tenant-001/stats
```

#### POST /api/tenants/{tenantId}/test-connection
**Description**: Test Gainsight PX API connection
```bash
curl -X POST http://localhost:8080/api/tenants/tenant-001/test-connection
```

### Monitoring Endpoints

#### GET /api/monitoring/health
**Description**: Application health status
```bash
curl -X GET http://localhost:8080/api/monitoring/health
```

#### GET /api/monitoring/metrics
**Description**: System metrics and statistics
```bash
curl -X GET http://localhost:8080/api/monitoring/metrics
```

#### GET /api/monitoring/status
**Description**: Detailed system status
```bash
curl -X GET http://localhost:8080/api/monitoring/status
```

## Usage Examples

### 1. Setting Up a New Tenant

```bash
# Step 1: Create tenant configuration
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-corp",
    "companyName": "Acme Corporation",
    "apiKey": "px-api-key-12345",
    "apiUrl": "https://api.aptrinsic.com",
    "active": false,
    "extractionIntervalMinutes": 5,
    "extractCustomEvents": true,
    "extractStandardEvents": true
  }'

# Step 2: Test connection
curl -X POST http://localhost:8080/api/tenants/acme-corp/test-connection

# Step 3: Activate if connection successful
curl -X POST http://localhost:8080/api/tenants/acme-corp/activate

# Step 4: Trigger manual extraction to test
curl -X POST http://localhost:8080/api/tenants/acme-corp/extract

# Step 5: Check extraction statistics
curl -X GET http://localhost:8080/api/tenants/acme-corp/stats
```

### 2. Monitoring System Health

```bash
# Check overall health
curl -X GET http://localhost:8080/api/monitoring/health

# Get detailed metrics
curl -X GET http://localhost:8080/api/monitoring/metrics | jq .

# Check specific tenant status
curl -X GET http://localhost:8080/api/tenants/tenant-001/stats | jq .
```

### 3. Bulk Operations

```bash
# Get all active tenants
curl -X GET http://localhost:8080/api/tenants | jq '.[] | select(.active == true)'

# Deactivate all tenants (for maintenance)
for tenant in $(curl -s http://localhost:8080/api/tenants | jq -r '.[].tenantId'); do
  curl -X POST http://localhost:8080/api/tenants/$tenant/deactivate
done
```

### 4. Database Operations

```sql
-- Access H2 Console at http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:mem:gainsightdb
-- Username: sa
-- Password: (leave empty)

-- View all tenant configurations
SELECT * FROM tenant_configurations;

-- View extracted events
SELECT * FROM extracted_events ORDER BY extracted_at DESC LIMIT 10;

-- Check extraction statistics
SELECT 
  tenant_id,
  COUNT(*) as total_events,
  MAX(extracted_at) as last_extraction,
  COUNT(CASE WHEN status = 'PROCESSED' THEN 1 END) as processed_events,
  COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_events
FROM extracted_events 
GROUP BY tenant_id;
```

## Configuration Guide

### Application Properties

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:gainsightdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# H2 Console (Development only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Gainsight PX Default Configuration
gainsight.px.default.api-url=https://api.aptrinsic.com
gainsight.px.default.timeout-seconds=30
gainsight.px.default.max-retry-attempts=3

# Logging Configuration
logging.level.org.example.gainsightapp=INFO
logging.level.org.springframework.retry=DEBUG
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Environment-Specific Configuration

#### Development
```properties
# application-dev.properties
spring.jpa.show-sql=true
logging.level.org.example.gainsightapp=DEBUG
spring.h2.console.enabled=true
```

#### Production
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gainsightdb
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.h2.console.enabled=false
logging.level.org.example.gainsightapp=WARN
```

### Tenant Configuration Options

| Field | Description | Default | Required |
|-------|-------------|---------|----------|
| `tenantId` | Unique identifier for tenant | - | Yes |
| `companyName` | Company display name | - | Yes |
| `apiKey` | Gainsight PX API key | - | Yes |
| `apiUrl` | Gainsight PX API base URL | `https://api.aptrinsic.com` | No |
| `active` | Enable/disable extraction | `true` | No |
| `extractionIntervalMinutes` | Extraction frequency | `5` | No |
| `extractCustomEvents` | Extract custom events | `true` | No |
| `extractStandardEvents` | Extract standard events | `true` | No |
| `maxRetryAttempts` | Max retry attempts on failure | `3` | No |
| `timeoutSeconds` | API request timeout | `30` | No |

## Monitoring & Troubleshooting

### Key Metrics to Monitor

1. **Tenant Health**
   - Active vs inactive tenants
   - Last successful extraction time
   - Error rates per tenant

2. **System Performance**
   - Total events extracted
   - Extraction success rate
   - Average extraction time

3. **Error Tracking**
   - Failed extraction attempts
   - Retry attempts
   - Connection failures

### Common Log Patterns

```bash
# Successful extraction
grep "Extracted.*events for tenant" logs/application.log

# Extraction errors
grep "Error extracting events" logs/application.log

# Retry attempts
grep "Retry.*attempt" logs/application.log

# Connection issues
grep "Connection test failed" logs/application.log
```

### Troubleshooting Steps

1. **Check Application Health**
   ```bash
   curl http://localhost:8080/api/monitoring/health
   ```

2. **Verify Tenant Configuration**
   ```bash
   curl http://localhost:8080/api/tenants/{tenantId}/test-connection
   ```

3. **Check Recent Extractions**
   ```bash
   curl http://localhost:8080/api/tenants/{tenantId}/stats
   ```

4. **Review Application Logs**
   ```bash
   tail -f logs/application.log | grep -E "(ERROR|WARN)"
   ```

## Important Notes

### 🔒 Security Considerations

1. **API Key Security**
   - Store API keys securely (consider encryption at rest)
   - Rotate API keys regularly
   - Never log API keys in plain text

2. **Database Security**
   - Use strong database credentials in production
   - Enable SSL/TLS for database connections
   - Implement proper access controls

3. **Network Security**
   - Use HTTPS for all external API calls
   - Implement proper firewall rules
   - Consider API rate limiting

### ⚡ Performance Considerations

1. **Extraction Frequency**
   - Balance between data freshness and API rate limits
   - Consider different intervals for different tenants
   - Monitor Gainsight PX API quotas

2. **Database Performance**
   - Index frequently queried columns
   - Archive old extracted events
   - Monitor database size and performance

3. **Memory Usage**
   - Monitor JVM heap usage during bulk extractions
   - Implement pagination for large event sets
   - Consider streaming for very large datasets

### 🔄 Data Consistency

1. **Duplicate Prevention**
   - System automatically prevents duplicate events
   - Uses event ID for deduplication
   - Logs duplicate attempts for monitoring

2. **Transaction Management**
   - Each tenant extraction runs in separate transaction
   - Failed extractions don't affect other tenants
   - Automatic rollback on errors

3. **State Management**
   - Tracks last processed scroll ID per tenant
   - Resumes extraction from last successful point
   - Maintains extraction history for auditing

### 📊 Scalability Notes

1. **Horizontal Scaling**
   - Consider multiple application instances
   - Implement distributed locking for scheduled tasks
   - Use external database for multi-instance deployment

2. **Vertical Scaling**
   - Monitor CPU and memory usage
   - Tune JVM parameters for production
   - Consider connection pooling optimizations

## Architecture Overview

### Component Diagram
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │    │   Service       │    │   Data Access   │
│   Controllers   │───▶│   Layer         │───▶│   Repositories  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         │              │   Integration   │              │
         └──────────────│   Layer         │──────────────┘
                        └─────────────────┘
                                │
                        ┌─────────────────┐
                        │   Gainsight PX  │
                        │   API           │
                        └─────────────────┘
```

### Data Flow
```
1. Scheduled Task ──▶ 2. Get Active Tenants ──▶ 3. For Each Tenant
                                                      │
4. Extract Events ◀── 5. Call Gainsight API ◀─────────┘
        │
        ▼
6. Store Events ──▶ 7. Update Tenant Status ──▶ 8. Log Results
```

## Development Guide

### Running Tests
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=GainsightAppApplicationTests
```

### Building for Production
```bash
# Build JAR
mvn clean package

# Build with production profile
mvn clean package -Pprod

# Build Docker image (if Dockerfile exists)
docker build -t gainsight-app .
```

### Code Quality
```bash
# Check code style
mvn checkstyle:check

# Static analysis
mvn spotbugs:check

# Dependency analysis
mvn dependency:analyze
```

## Production Deployment

### Pre-deployment Checklist

- [ ] Database migration scripts prepared
- [ ] Environment-specific configuration files ready
- [ ] API keys and credentials secured
- [ ] Monitoring and alerting configured
- [ ] Backup and recovery procedures documented
- [ ] Performance testing completed
- [ ] Security review completed

### Deployment Steps

1. **Database Setup**
   ```sql
   -- Create production database
   CREATE DATABASE gainsightdb;
   CREATE USER gainsight_user WITH PASSWORD 'secure_password';
   GRANT ALL PRIVILEGES ON DATABASE gainsightdb TO gainsight_user;
   ```

2. **Application Configuration**
   ```bash
   # Set environment variables
   export DB_USERNAME=gainsight_user
   export DB_PASSWORD=secure_password
   export SPRING_PROFILES_ACTIVE=prod
   ```

3. **Start Application**
   ```bash
   java -jar gainsight-app.jar --spring.profiles.active=prod
   ```

### Health Checks

```bash
# Application health
curl -f http://localhost:8080/api/monitoring/health || exit 1

# Database connectivity
curl -f http://localhost:8080/api/monitoring/status || exit 1
```

## FAQ & Common Issues

### Q: Application fails to start with "Unable to find @SpringBootConfiguration"
**A**: This usually happens in test environments. Ensure the test class specifies the main application class:
```java
@SpringBootTest(classes = GainsightAppApplication.class)
```

### Q: Getting "Connection refused" errors when calling Gainsight API
**A**: Check:
1. API key is valid
2. Network connectivity to api.aptrinsic.com
3. Firewall rules allow outbound HTTPS
4. Tenant configuration is correct

### Q: Scheduled tasks are not running
**A**: Verify:
1. `@EnableScheduling` annotation is present on main class
2. Application is running (not just started and stopped)
3. Check logs for scheduling-related errors

### Q: High memory usage during extraction
**A**: Consider:
1. Implementing pagination for large event sets
2. Increasing JVM heap size
3. Archiving old extracted events
4. Processing events in smaller batches

### Q: Duplicate events being created
**A**: This shouldn't happen as the system has duplicate prevention. If it does:
1. Check event ID consistency from Gainsight API
2. Verify database constraints are in place
3. Review extraction logic for race conditions

### Q: Tenant extraction stuck in "PROCESSING" state
**A**: This indicates an incomplete extraction. To resolve:
1. Check application logs for errors
2. Restart the application if necessary
3. Consider implementing timeout mechanisms for long-running extractions

---

## Support & Maintenance

For ongoing support and maintenance:
- Monitor application logs regularly
- Keep dependencies updated
- Review and rotate API keys periodically
- Archive old extracted events to manage database size
- Performance tune based on actual usage patterns

**Last Updated**: July 13, 2025
**Version**: 1.0.0
