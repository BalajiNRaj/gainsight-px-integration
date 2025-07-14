# MongoDB Setup Scripts

This directory contains scripts to help you set up and manage your MongoDB database for the Gainsight PX Integration application.

## setup-mongodb.js

This script creates the necessary indexes and sample data for your MongoDB database.

### Usage

#### For MongoDB Atlas:
```bash
# Connect to your Atlas cluster and run the script
mongosh "mongodb+srv://username:password@cluster.mongodb.net/gainsightdb" --file setup-mongodb.js
```

#### For Local MongoDB:
```bash
# Connect to local MongoDB and run the script
mongosh "mongodb://localhost:27017/gainsightdb" --file setup-mongodb.js
```

### What this script does:
1. Creates necessary collections
2. Sets up optimal indexes for performance
3. Inserts a sample tenant configuration (inactive by default)
4. Displays current index configuration

### Manual Setup (Alternative)

If you prefer to run commands manually, connect to your MongoDB instance:

```bash
mongosh "your_connection_string"
```

Then run:
```javascript
use gainsightdb;

// Create unique index on tenantId
db.tenant_configurations.createIndex({tenantId: 1}, {unique: true});

// Create performance indexes
db.extracted_events.createIndex({tenantId: 1, extractedAt: 1});
db.extracted_events.createIndex({status: 1});
db.extracted_events.createIndex({tenantId: 1, eventId: 1}, {unique: true});
```

## Maintenance Commands

### Clean old events (older than 30 days):
```javascript
db.extracted_events.deleteMany({
  extractedAt: {
    $lt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  }
});
```

### Check database stats:
```javascript
db.stats();
db.tenant_configurations.stats();
db.extracted_events.stats();
```

### Monitor query performance:
```javascript
db.extracted_events.find({tenantId: "tenant-001"}).explain("executionStats");
```
