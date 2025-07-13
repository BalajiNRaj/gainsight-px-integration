package org.example.gainsightapp.controller;

import org.example.gainsightapp.model.TenantConfiguration;
import org.example.gainsightapp.repository.ExtractedEventRepository;
import org.example.gainsightapp.repository.TenantConfigurationRepository;
import org.example.gainsightapp.service.TenantManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    private final TenantConfigurationRepository tenantRepository;
    private final ExtractedEventRepository eventRepository;
    private final TenantManagementService tenantService;
    
    public MonitoringController(
            TenantConfigurationRepository tenantRepository,
            ExtractedEventRepository eventRepository,
            TenantManagementService tenantService) {
        this.tenantRepository = tenantRepository;
        this.eventRepository = eventRepository;
        this.tenantService = tenantService;
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic service health
            long totalTenants = tenantRepository.count();
            long activeTenants = tenantRepository.findByActiveTrue().size();
            long totalEvents = eventRepository.count();
            
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("totalTenants", totalTenants);
            health.put("activeTenants", activeTenants);
            health.put("totalEvents", totalEvents);
            
            // Check for tenants with errors
            List<TenantConfiguration> tenantsWithErrors = tenantService.getTenantsWithErrors();
            health.put("tenantsWithErrors", tenantsWithErrors.size());
            
            if (!tenantsWithErrors.isEmpty()) {
                health.put("status", "DEGRADED");
                health.put("errorDetails", tenantsWithErrors.stream()
                    .map(t -> Map.of(
                        "tenantId", t.getTenantId(),
                        "error", t.getLastExtractionError(),
                        "lastAttempt", t.getLastAttemptedExtraction()
                    )).toList());
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            
            // Tenant metrics
            long totalTenants = tenantRepository.count();
            long activeTenants = tenantRepository.findByActiveTrue().size();
            
            metrics.put("tenants", Map.of(
                "total", totalTenants,
                "active", activeTenants,
                "inactive", totalTenants - activeTenants
            ));
            
            // Event metrics
            long totalEvents = eventRepository.count();
            
            metrics.put("events", Map.of(
                "total", totalEvents,
                "last24Hours", totalEvents, // Simplified for demo
                "lastHour", totalEvents // Simplified for demo
            ));
            
            // Extraction status
            List<TenantConfiguration> allTenants = tenantRepository.findAll();
            long successfulExtractions = allTenants.stream()
                .filter(t -> t.getLastSuccessfulExtraction() != null)
                .filter(t -> t.getLastSuccessfulExtraction().isAfter(last24Hours))
                .count();
            
            long failedExtractions = allTenants.stream()
                .filter(t -> t.getLastExtractionError() != null)
                .count();
            
            metrics.put("extractions", Map.of(
                "successful24h", successfulExtractions,
                "failed", failedExtractions
            ));
            
            metrics.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // System info
            status.put("timestamp", LocalDateTime.now());
            status.put("uptime", "Running"); // Simplified
            status.put("version", "1.0.0");
            
            // Database connectivity
            try {
                tenantRepository.count();
                status.put("database", "CONNECTED");
            } catch (Exception e) {
                status.put("database", "DISCONNECTED");
                status.put("databaseError", e.getMessage());
            }
            
            // Gainsight PX connectivity (test with first active tenant)
            List<TenantConfiguration> activeTenants = tenantRepository.findByActiveTrue();
            if (!activeTenants.isEmpty()) {
                TenantConfiguration firstTenant = activeTenants.get(0);
                boolean connectionValid = tenantService.testTenantConnection(firstTenant.getTenantId());
                status.put("gainsightPX", connectionValid ? "CONNECTED" : "DISCONNECTED");
            } else {
                status.put("gainsightPX", "NO_ACTIVE_TENANTS");
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
}
