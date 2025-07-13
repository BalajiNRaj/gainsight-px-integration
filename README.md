# Gainsight PX Multi-Tenant Integration

A robust Java Spring Boot application for periodically extracting events from multiple Gainsight PX accounts with comprehensive error handling, retry mechanisms, and multi-tenancy support.

## Features

### 🏢 Multi-Tenancy Support
- Configure multiple company accounts with individual Gainsight PX credentials
- Per-tenant extraction preferences and intervals
- Isolated data storage per tenant
- Independent error handling and retry logic

### 🔄 Reliable Data Extraction
- Scheduled extraction every 5 minutes (configurable per tenant)
- Support for both custom and standard events
- Pagination with scroll-based API handling
- Automatic retry mechanisms with exponential backoff
- Resume capability for interrupted extractions

### 🛡️ Error Handling & Monitoring
- Comprehensive error logging and diagnostics
- Health check endpoints for monitoring
- Metrics and statistics endpoints
- Connection testing for tenant configurations
- Graceful handling of API rate limits and timeouts

### 📈 Scalability & Performance
- Concurrent extraction for multiple tenants
- Configurable thread pool for parallel processing
- Efficient database storage with JPA/Hibernate
- Memory-optimized event processing

## Architecture

### Core Components

1. **TenantConfiguration** - Entity for storing tenant-specific settings
2. **ExtractedEvent** - Entity for storing extracted events
3. **MultiTenantGainsightPXClient** - HTTP client with retry logic
4. **GainsightEventExtractionService** - Core extraction logic
5. **TenantManagementService** - Tenant CRUD operations
6. **GainsightScheduledTaskService** - Scheduled extraction tasks

### Database Schema

- `tenant_configurations` - Tenant settings and credentials
- `extracted_events` - Extracted event data with processing status

## 📚 Documentation

This project includes comprehensive documentation:

- **[USAGE_GUIDE.md](USAGE_GUIDE.md)** - Complete usage guide with API reference, examples, configuration, troubleshooting, and best practices
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick reference card with commonly used commands and endpoints
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Detailed deployment and operations guide for production environments

### Quick Links
- **API Endpoints**: See [USAGE_GUIDE.md#api-reference](USAGE_GUIDE.md#api-reference)
- **Configuration**: See [USAGE_GUIDE.md#configuration-guide](USAGE_GUIDE.md#configuration-guide)
- **Troubleshooting**: See [USAGE_GUIDE.md#monitoring--troubleshooting](USAGE_GUIDE.md#monitoring--troubleshooting)
- **Production Deployment**: See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Spring Boot 3.5.3

### Installation

1. Clone the repository
2. Update `application.properties` with your configuration
3. Build and run the application:

```bash
mvn clean install
mvn spring-boot:run
```

### Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:gainsightdb
spring.datasource.username=sa
spring.datasource.password=password

# Default Gainsight PX Configuration
gainsight.px.api-url=https://api.aptrinsic.com
gainsight.px.api-key=your-api-key-here
```

## API Endpoints

### Tenant Management

- `POST /api/tenants` - Create new tenant
- `GET /api/tenants` - List all tenants
- `GET /api/tenants/{tenantId}` - Get tenant details
- `PUT /api/tenants/{tenantId}` - Update tenant
- `DELETE /api/tenants/{tenantId}` - Delete tenant
- `POST /api/tenants/{tenantId}/activate` - Activate tenant
- `POST /api/tenants/{tenantId}/deactivate` - Deactivate tenant
- `POST /api/tenants/{tenantId}/test-connection` - Test Gainsight PX connection
- `POST /api/tenants/{tenantId}/extract` - Trigger manual extraction
- `GET /api/tenants/{tenantId}/events` - Get tenant events (paginated)
- `GET /api/tenants/{tenantId}/stats` - Get tenant statistics

### Monitoring

- `GET /api/monitoring/health` - Application health status
- `GET /api/monitoring/metrics` - System metrics
- `GET /api/monitoring/status` - Detailed system status

### Development Tools

- `GET /h2-console` - H2 Database Console (development only)

## Usage Examples

### Creating a New Tenant

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "company-123",
    "companyName": "Example Corp",
    "apiKey": "your-gainsight-api-key",
    "apiUrl": "https://api.aptrinsic.com",
    "extractionIntervalMinutes": 5,
    "extractCustomEvents": true,
    "extractStandardEvents": true,
    "active": true
  }'
```

### Testing Connection

```bash
curl -X POST http://localhost:8080/api/tenants/company-123/test-connection
```

### Checking Health Status

```bash
curl http://localhost:8080/api/monitoring/health
```

### Getting Tenant Statistics

```bash
curl http://localhost:8080/api/tenants/company-123/stats
```

## Data Extraction Process

1. **Scheduled Execution**: Every 5 minutes, the scheduler checks all active tenants
2. **Connection Testing**: Validates API connectivity before extraction
3. **Event Fetching**: 
   - Fetches custom events (if enabled)
   - Fetches standard events (if enabled)
   - Uses pagination with scrollId for large datasets
4. **Data Processing**: 
   - Parses event data and extracts metadata
   - Checks for duplicates
   - Stores in database with processing status
5. **Error Handling**: 
   - Logs errors with context
   - Updates tenant status
   - Continues with other tenants

## Configuration Options

### Per-Tenant Settings

- `extractionIntervalMinutes` - How often to extract (default: 5)
- `extractCustomEvents` - Extract custom events (default: true)
- `extractStandardEvents` - Extract standard events (default: true)
- `maxRetryAttempts` - Maximum retry attempts (default: 3)
- `timeoutSeconds` - Request timeout (default: 30)

### System Settings

- Thread pool size: 10 concurrent extractions
- Page size: 100 events per request
- Maximum pages per extraction: 100 (safety limit)
- Rate limiting: 100ms delay between requests

## Error Handling

### Retry Logic
- Automatic retries for transient errors
- Exponential backoff strategy
- Separate retry counts per tenant

### Error Types Handled
- Network connectivity issues
- API rate limiting
- Authentication failures
- Malformed response data
- Database connectivity issues

### Monitoring & Alerting
- Health endpoints for external monitoring
- Detailed error logging
- Tenant-specific error tracking
- Metrics for monitoring dashboards

## Security Considerations

- API keys stored securely per tenant
- Connection validation before activation
- Input validation for all endpoints
- Secure handling of sensitive data

## Troubleshooting

### Common Issues

1. **Connection Test Failures**
   - Verify API key is correct
   - Check API URL format
   - Ensure network connectivity

2. **Extraction Errors**
   - Check logs for specific error messages
   - Verify tenant is active
   - Test API connectivity

3. **Performance Issues**
   - Monitor thread pool utilization
   - Check database performance
   - Review extraction intervals

### Debugging

- Enable debug logging: `logging.level.org.example.gainsightapp=DEBUG`
- Check H2 console for database state
- Use monitoring endpoints for system health

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.
