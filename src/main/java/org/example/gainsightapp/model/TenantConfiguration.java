package org.example.gainsightapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_configurations")
public class TenantConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique = true)
    private String tenantId;
    
    @NotBlank
    private String companyName;
    
    @NotBlank
    private String apiKey;
    
    @NotBlank
    private String apiUrl;
    
    @NotNull
    private Boolean active = true;
    
    // Extraction preferences
    private Integer extractionIntervalMinutes = 5;
    private Boolean extractCustomEvents = true;
    private Boolean extractStandardEvents = true;
    private Integer maxRetryAttempts = 3;
    private Integer timeoutSeconds = 30;
    
    // Last extraction tracking
    private LocalDateTime lastSuccessfulExtraction;
    private LocalDateTime lastAttemptedExtraction;
    private String lastExtractionError;
    private String lastProcessedScrollId;
    
    // Timestamps
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public TenantConfiguration() {}
    
    public TenantConfiguration(String tenantId, String companyName, String apiKey, String apiUrl) {
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Integer getExtractionIntervalMinutes() { return extractionIntervalMinutes; }
    public void setExtractionIntervalMinutes(Integer extractionIntervalMinutes) { this.extractionIntervalMinutes = extractionIntervalMinutes; }
    
    public Boolean getExtractCustomEvents() { return extractCustomEvents; }
    public void setExtractCustomEvents(Boolean extractCustomEvents) { this.extractCustomEvents = extractCustomEvents; }
    
    public Boolean getExtractStandardEvents() { return extractStandardEvents; }
    public void setExtractStandardEvents(Boolean extractStandardEvents) { this.extractStandardEvents = extractStandardEvents; }
    
    public Integer getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(Integer maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public LocalDateTime getLastSuccessfulExtraction() { return lastSuccessfulExtraction; }
    public void setLastSuccessfulExtraction(LocalDateTime lastSuccessfulExtraction) { this.lastSuccessfulExtraction = lastSuccessfulExtraction; }
    
    public LocalDateTime getLastAttemptedExtraction() { return lastAttemptedExtraction; }
    public void setLastAttemptedExtraction(LocalDateTime lastAttemptedExtraction) { this.lastAttemptedExtraction = lastAttemptedExtraction; }
    
    public String getLastExtractionError() { return lastExtractionError; }
    public void setLastExtractionError(String lastExtractionError) { this.lastExtractionError = lastExtractionError; }
    
    public String getLastProcessedScrollId() { return lastProcessedScrollId; }
    public void setLastProcessedScrollId(String lastProcessedScrollId) { this.lastProcessedScrollId = lastProcessedScrollId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
