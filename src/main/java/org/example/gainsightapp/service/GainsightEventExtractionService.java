package org.example.gainsightapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gainsightapp.integration.MultiTenantGainsightPXClient;
import org.example.gainsightapp.model.ExtractedEvent;
import org.example.gainsightapp.model.TenantConfiguration;
import org.example.gainsightapp.repository.ExtractedEventRepository;
import org.example.gainsightapp.repository.TenantConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GainsightEventExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(GainsightEventExtractionService.class);
    
    private final MultiTenantGainsightPXClient gainsightClient;
    private final TenantConfigurationRepository tenantRepository;
    private final ExtractedEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    public GainsightEventExtractionService(
            MultiTenantGainsightPXClient gainsightClient,
            TenantConfigurationRepository tenantRepository,
            ExtractedEventRepository eventRepository,
            ObjectMapper objectMapper) {
        this.gainsightClient = gainsightClient;
        this.tenantRepository = tenantRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(10); // Configurable pool size
    }
    
    @Transactional
    public void extractEventsForAllTenants() {
        logger.info("Starting event extraction for all active tenants");
        
        List<TenantConfiguration> activeTenants = tenantRepository.findByActiveTrue();
        logger.info("Found {} active tenants", activeTenants.size());
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (TenantConfiguration tenant : activeTenants) {
            if (shouldExtractForTenant(tenant)) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> extractEventsForTenant(tenant), executorService);
                futures.add(future);
            }
        }
        
        // Wait for all extractions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
        
        logger.info("Completed event extraction for all tenants");
    }
    
    @Transactional
    public void extractEventsForTenant(TenantConfiguration tenant) {
        logger.info("Starting event extraction for tenant: {}", tenant.getTenantId());
        
        try {
            // Update attempt timestamp
            tenant.setLastAttemptedExtraction(LocalDateTime.now());
            tenantRepository.save(tenant);
            
            // Test connection first
            if (!gainsightClient.testConnection(tenant)) {
                throw new RuntimeException("Connection test failed");
            }
            
            int totalExtracted = 0;
            
            // Extract custom events if enabled
            if (tenant.getExtractCustomEvents()) {
                int customEventsExtracted = extractEventsByType(tenant, "CUSTOM");
                totalExtracted += customEventsExtracted;
                logger.info("Extracted {} custom events for tenant: {}", 
                           customEventsExtracted, tenant.getTenantId());
            }
            
            // Extract standard events if enabled
            if (tenant.getExtractStandardEvents()) {
                int standardEventsExtracted = extractEventsByType(tenant, "STANDARD");
                totalExtracted += standardEventsExtracted;
                logger.info("Extracted {} standard events for tenant: {}", 
                           standardEventsExtracted, tenant.getTenantId());
            }
            
            // Update success status
            tenant.setLastSuccessfulExtraction(LocalDateTime.now());
            tenant.setLastExtractionError(null);
            tenantRepository.save(tenant);
            
            logger.info("Successfully extracted {} total events for tenant: {}", 
                       totalExtracted, tenant.getTenantId());
            
        } catch (Exception e) {
            logger.error("Error extracting events for tenant {}: {}", 
                        tenant.getTenantId(), e.getMessage(), e);
            
            // Update error status
            tenant.setLastExtractionError(e.getMessage());
            tenantRepository.save(tenant);
            
            // Don't rethrow to allow other tenants to continue
        }
    }
    
    private int extractEventsByType(TenantConfiguration tenant, String eventType) {
        int totalExtracted = 0;
        String scrollId = tenant.getLastProcessedScrollId();
        boolean hasMore = true;
        int pageSize = 100; // Configurable
        int maxPages = 100; // Safety limit
        int pageCount = 0;
        
        try {
            while (hasMore && pageCount < maxPages) {
                MultiTenantGainsightPXClient.GainsightResponse response = 
                    gainsightClient.fetchEvents(tenant, eventType, scrollId, pageSize);
                
                if (!response.isSuccess()) {
                    throw new RuntimeException("API request failed with status: " + response.getStatusCode());
                }
                
                JsonNode events = response.getData();
                if (events != null && events.isArray()) {
                    List<ExtractedEvent> newEvents = processEvents(tenant, events, eventType);
                    if (!newEvents.isEmpty()) {
                        eventRepository.saveAll(newEvents);
                        totalExtracted += newEvents.size();
                        logger.debug("Saved {} {} events for tenant: {}", 
                                   newEvents.size(), eventType, tenant.getTenantId());
                    }
                }
                
                // Update pagination
                scrollId = response.getScrollId();
                hasMore = response.isHasMore() && scrollId != null;
                pageCount++;
                
                // Update scroll ID for resumption
                tenant.setLastProcessedScrollId(scrollId);
                tenantRepository.save(tenant);
                
                // Rate limiting
                if (hasMore) {
                    Thread.sleep(100); // 100ms delay between requests
                }
            }
            
            if (pageCount >= maxPages) {
                logger.warn("Reached maximum page limit ({}) for tenant: {} event type: {}", 
                           maxPages, tenant.getTenantId(), eventType);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Extraction interrupted", e);
        }
        
        return totalExtracted;
    }
    
    private List<ExtractedEvent> processEvents(TenantConfiguration tenant, JsonNode events, String eventType) {
        List<ExtractedEvent> extractedEvents = new ArrayList<>();
        
        for (JsonNode event : events) {
            try {
                String eventId = extractEventId(event);
                if (eventId == null) {
                    logger.warn("Skipping event without ID for tenant: {}", tenant.getTenantId());
                    continue;
                }
                
                // Check for duplicates
                if (eventRepository.existsByTenantIdAndEventId(tenant.getTenantId(), eventId)) {
                    logger.debug("Skipping duplicate event {} for tenant: {}", eventId, tenant.getTenantId());
                    continue;
                }
                
                ExtractedEvent extractedEvent = new ExtractedEvent();
                extractedEvent.setTenantId(tenant.getTenantId());
                extractedEvent.setEventId(eventId);
                extractedEvent.setEventType(eventType);
                extractedEvent.setEventName(extractEventName(event));
                extractedEvent.setEventData(objectMapper.writeValueAsString(event));
                extractedEvent.setEventTimestamp(extractEventTimestamp(event));
                
                extractedEvents.add(extractedEvent);
                
            } catch (Exception e) {
                logger.error("Error processing event for tenant {}: {}", 
                           tenant.getTenantId(), e.getMessage(), e);
                // Continue processing other events
            }
        }
        
        return extractedEvents;
    }
    
    private String extractEventId(JsonNode event) {
        // Try common ID fields
        String[] idFields = {"id", "eventId", "globalContext.eventId", "_id"};
        
        for (String field : idFields) {
            JsonNode idNode = event.at("/" + field.replace(".", "/"));
            if (idNode != null && !idNode.isMissingNode() && !idNode.isNull()) {
                return idNode.asText();
            }
        }
        
        return null;
    }
    
    private String extractEventName(JsonNode event) {
        // Try common name fields
        String[] nameFields = {"eventName", "name", "type", "eventType"};
        
        for (String field : nameFields) {
            JsonNode nameNode = event.at("/" + field.replace(".", "/"));
            if (nameNode != null && !nameNode.isMissingNode() && !nameNode.isNull()) {
                return nameNode.asText();
            }
        }
        
        return "unknown";
    }
    
    private LocalDateTime extractEventTimestamp(JsonNode event) {
        // Try common timestamp fields
        String[] timestampFields = {"timestamp", "eventTime", "createdAt", "occurred"};
        
        for (String field : timestampFields) {
            JsonNode timestampNode = event.at("/" + field.replace(".", "/"));
            if (timestampNode != null && !timestampNode.isMissingNode() && !timestampNode.isNull()) {
                try {
                    String timestampStr = timestampNode.asText();
                    // Try different timestamp formats
                    return parseTimestamp(timestampStr);
                } catch (Exception e) {
                    logger.debug("Failed to parse timestamp field {}: {}", field, e.getMessage());
                }
            }
        }
        
        // Default to current time if no timestamp found
        return LocalDateTime.now();
    }
    
    private LocalDateTime parseTimestamp(String timestamp) {
        // Try different common formats
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss"
        };
        
        for (String format : formats) {
            try {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        // Try parsing as epoch milliseconds
        try {
            long epochMilli = Long.parseLong(timestamp);
            return LocalDateTime.ofEpochSecond(epochMilli / 1000, 0, java.time.ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            // Fall back to current time
        }
        
        throw new DateTimeParseException("Unable to parse timestamp: " + timestamp, timestamp, 0);
    }
    
    private boolean shouldExtractForTenant(TenantConfiguration tenant) {
        if (!tenant.getActive()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastAttempt = tenant.getLastAttemptedExtraction();
        
        if (lastAttempt == null) {
            return true; // First extraction
        }
        
        // Check if enough time has passed since last attempt
        long minutesSinceLastAttempt = java.time.Duration.between(lastAttempt, now).toMinutes();
        return minutesSinceLastAttempt >= tenant.getExtractionIntervalMinutes();
    }
}
