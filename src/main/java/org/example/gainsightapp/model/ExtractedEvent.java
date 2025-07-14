package org.example.gainsightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "extracted_events")
public class ExtractedEvent {
    
    @Id
    private String id;
    
    @Indexed
    private String tenantId;
    
    @Indexed
    private String eventId;
    
    private String eventType; // CUSTOM or STANDARD
    private String eventName;
    private String eventData; // JSON string
    private LocalDateTime eventTimestamp;
    
    @Indexed
    private LocalDateTime extractedAt;
    
    // Processing status
    @Indexed
    private ProcessingStatus status = ProcessingStatus.EXTRACTED;
    
    private String processingError;
    private Integer retryCount = 0;
    
    public void onCreate() {
        if (extractedAt == null) {
            extractedAt = LocalDateTime.now();
        }
    }
    
    public enum ProcessingStatus {
        EXTRACTED, PROCESSING, PROCESSED, FAILED
    }
    
    // Constructors
    public ExtractedEvent() {}
    
    public ExtractedEvent(String tenantId, String eventId, String eventType, 
                         String eventName, String eventData, LocalDateTime eventTimestamp) {
        this.tenantId = tenantId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventName = eventName;
        this.eventData = eventData;
        this.eventTimestamp = eventTimestamp;
        onCreate();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    
    public LocalDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(LocalDateTime extractedAt) { this.extractedAt = extractedAt; }
    
    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }
    
    public String getProcessingError() { return processingError; }
    public void setProcessingError(String processingError) { this.processingError = processingError; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}
