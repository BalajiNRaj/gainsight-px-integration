package org.example.gainsightapp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gainsightapp.model.TenantConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
public class MultiTenantGainsightPXClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantGainsightPXClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public MultiTenantGainsightPXClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Retryable(value = {ResourceAccessException.class, HttpClientErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public GainsightResponse fetchEvents(TenantConfiguration tenant, String eventType, 
                                       String scrollId, Integer pageSize) {
        try {
            String url = buildEventsUrl(tenant.getApiUrl(), eventType);
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            if (pageSize != null && pageSize > 0) {
                builder.queryParam("pageSize", Math.min(pageSize, 1000)); // Limit max page size
            }
            if (scrollId != null && !scrollId.isEmpty()) {
                builder.queryParam("scrollId", scrollId);
            }
            
            // Add date filter for recent events (last 24 hours if first extraction)
            if (scrollId == null && tenant.getLastSuccessfulExtraction() != null) {
                String fromDate = tenant.getLastSuccessfulExtraction()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                builder.queryParam("from", fromDate);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(createHeaders(tenant));
            
            logger.debug("Fetching {} events for tenant: {} from URL: {}", 
                        eventType, tenant.getTenantId(), builder.toUriString());
            
            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), 
                HttpMethod.GET, 
                request, 
                String.class
            );
            
            return parseGainsightResponse(response);
            
        } catch (Exception e) {
            logger.error("Error fetching {} events for tenant {}: {}", 
                        eventType, tenant.getTenantId(), e.getMessage(), e);
            throw new GainsightPXException("Failed to fetch events", e);
        }
    }
    
    @Retryable(value = {ResourceAccessException.class, HttpClientErrorException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public GainsightResponse fetchUsers(TenantConfiguration tenant, String scrollId, Integer pageSize) {
        try {
            String url = tenant.getApiUrl() + "/v1/users";
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            if (pageSize != null && pageSize > 0) {
                builder.queryParam("pageSize", Math.min(pageSize, 1000));
            }
            if (scrollId != null && !scrollId.isEmpty()) {
                builder.queryParam("scrollId", scrollId);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(createHeaders(tenant));
            
            logger.debug("Fetching users for tenant: {} from URL: {}", 
                        tenant.getTenantId(), builder.toUriString());
            
            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), 
                HttpMethod.GET, 
                request, 
                String.class
            );
            
            return parseGainsightResponse(response);
            
        } catch (Exception e) {
            logger.error("Error fetching users for tenant {}: {}", 
                        tenant.getTenantId(), e.getMessage(), e);
            throw new GainsightPXException("Failed to fetch users", e);
        }
    }
    
    public boolean testConnection(TenantConfiguration tenant) {
        try {
            String url = tenant.getApiUrl() + "/v1/users?pageSize=1";
            HttpEntity<Void> request = new HttpEntity<>(createHeaders(tenant));
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.warn("Connection test failed for tenant {}: {}", 
                       tenant.getTenantId(), e.getMessage());
            return false;
        }
    }
    
    private String buildEventsUrl(String baseUrl, String eventType) {
        return switch (eventType.toUpperCase()) {
            case "CUSTOM" -> baseUrl + "/v1/events/custom";
            case "STANDARD" -> baseUrl + "/v1/events";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
    
    private HttpHeaders createHeaders(TenantConfiguration tenant) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tenant.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
    
    private GainsightResponse parseGainsightResponse(ResponseEntity<String> response) {
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            
            GainsightResponse gainsightResponse = new GainsightResponse();
            gainsightResponse.setSuccess(response.getStatusCode().is2xxSuccessful());
            gainsightResponse.setStatusCode(response.getStatusCode().value());
            gainsightResponse.setRawResponse(response.getBody());
            
            // Extract common fields
            if (root.has("scrollId")) {
                JsonNode scrollIdNode = root.get("scrollId");
                if (!scrollIdNode.isNull()) {
                    gainsightResponse.setScrollId(scrollIdNode.asText());
                }
            }
            
            if (root.has("hasMore")) {
                gainsightResponse.setHasMore(root.get("hasMore").asBoolean());
            }
            
            // Extract data array - handle multiple possible field names
            if (root.has("data") && root.get("data").isArray()) {
                gainsightResponse.setData(root.get("data"));
            } else if (root.has("customEvents") && root.get("customEvents").isArray()) {
                gainsightResponse.setData(root.get("customEvents"));
            } else if (root.has("users") && root.get("users").isArray()) {
                gainsightResponse.setData(root.get("users"));
            } else if (root.isArray()) {
                gainsightResponse.setData(root);
            }
            
            return gainsightResponse;
            
        } catch (Exception e) {
            logger.error("Error parsing Gainsight response: {}", e.getMessage(), e);
            throw new GainsightPXException("Failed to parse response", e);
        }
    }
    
    public static class GainsightResponse {
        private boolean success;
        private int statusCode;
        private String rawResponse;
        private JsonNode data;
        private String scrollId;
        private boolean hasMore;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
        
        public JsonNode getData() { return data; }
        public void setData(JsonNode data) { this.data = data; }
        
        public String getScrollId() { return scrollId; }
        public void setScrollId(String scrollId) { this.scrollId = scrollId; }
        
        public boolean isHasMore() { return hasMore; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class GainsightPXException extends RuntimeException {
        public GainsightPXException(String message) {
            super(message);
        }
        
        public GainsightPXException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
