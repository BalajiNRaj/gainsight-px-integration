// MongoDB Setup Script for Gainsight PX Integration
// Run this script after creating your MongoDB database

// Connect to your database
// use gainsightdb;

// Create collections explicitly (optional - will be created automatically)
db.createCollection("tenant_configurations");
db.createCollection("extracted_events");

// Create indexes for tenant_configurations collection
print("Creating indexes for tenant_configurations...");

// Unique index on tenantId
db.tenant_configurations.createIndex(
  { "tenantId": 1 }, 
  { unique: true, name: "idx_tenant_id_unique" }
);

// Index on active field for filtering
db.tenant_configurations.createIndex(
  { "active": 1 }, 
  { name: "idx_active" }
);

// Compound index for extraction scheduling
db.tenant_configurations.createIndex(
  { "active": 1, "lastAttemptedExtraction": 1 }, 
  { name: "idx_active_last_attempted" }
);

// Index for error tracking
db.tenant_configurations.createIndex(
  { "lastExtractionError": 1 }, 
  { sparse: true, name: "idx_extraction_error" }
);

// Create indexes for extracted_events collection
print("Creating indexes for extracted_events...");

// Index on tenantId for filtering by tenant
db.extracted_events.createIndex(
  { "tenantId": 1 }, 
  { name: "idx_tenant_id" }
);

// Index on eventId for duplicate detection
db.extracted_events.createIndex(
  { "eventId": 1 }, 
  { name: "idx_event_id" }
);

// Index on extractedAt for time-based queries
db.extracted_events.createIndex(
  { "extractedAt": 1 }, 
  { name: "idx_extracted_at" }
);

// Index on status for filtering by processing status
db.extracted_events.createIndex(
  { "status": 1 }, 
  { name: "idx_status" }
);

// Compound index for tenant-specific time queries
db.extracted_events.createIndex(
  { "tenantId": 1, "extractedAt": 1 }, 
  { name: "idx_tenant_extracted_at" }
);

// Compound index for duplicate detection
db.extracted_events.createIndex(
  { "tenantId": 1, "eventId": 1 }, 
  { unique: true, name: "idx_tenant_event_unique" }
);

// Index for retry logic
db.extracted_events.createIndex(
  { "status": 1, "retryCount": 1 }, 
  { name: "idx_status_retry" }
);

// Print current indexes
print("\nIndexes for tenant_configurations:");
db.tenant_configurations.getIndexes().forEach(function(index) {
  print("  - " + index.name + ": " + JSON.stringify(index.key));
});

print("\nIndexes for extracted_events:");
db.extracted_events.getIndexes().forEach(function(index) {
  print("  - " + index.name + ": " + JSON.stringify(index.key));
});

// Insert sample tenant configuration (optional)
print("\nInserting sample tenant configuration...");
try {
  db.tenant_configurations.insertOne({
    tenantId: "demo-tenant",
    companyName: "Demo Company",
    apiKey: "demo-api-key-replace-with-real",
    apiUrl: "https://api.aptrinsic.com",
    active: false, // Keep inactive for demo
    extractionIntervalMinutes: 5,
    extractCustomEvents: true,
    extractStandardEvents: true,
    maxRetryAttempts: 3,
    timeoutSeconds: 30,
    createdAt: new Date(),
    updatedAt: new Date()
  });
  print("Sample tenant configuration created successfully.");
} catch (e) {
  if (e.code === 11000) {
    print("Sample tenant already exists, skipping.");
  } else {
    print("Error creating sample tenant: " + e.message);
  }
}

print("\nMongoDB setup completed successfully!");
print("Next steps:");
print("1. Update your application.properties with the MongoDB connection string");
print("2. Replace the demo API key with your real Gainsight PX API key");
print("3. Start your Spring Boot application");
