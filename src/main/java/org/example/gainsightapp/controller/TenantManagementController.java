package org.example.gainsightapp.controller;

import org.example.gainsightapp.model.ExtractedEvent;
import org.example.gainsightapp.model.TenantConfiguration;
import org.example.gainsightapp.repository.ExtractedEventRepository;
import org.example.gainsightapp.service.GainsightEventExtractionService;
import org.example.gainsightapp.service.TenantManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
public class TenantManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantManagementController.class);
    
    private final TenantManagementService tenantService;
    private final GainsightEventExtractionService extractionService;
    private final ExtractedEventRepository eventRepository;
    
    public TenantManagementController(
            TenantManagementService tenantService,
            GainsightEventExtractionService extractionService,
            ExtractedEventRepository eventRepository) {
        this.tenantService = tenantService;
        this.extractionService = extractionService;
        this.eventRepository = eventRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createTenant(@Valid @RequestBody TenantConfiguration tenant) {
        try {
            TenantConfiguration createdTenant = tenantService.createTenant(tenant);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating tenant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TenantConfiguration>> getAllTenants() {
        List<TenantConfiguration> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }
    
    @GetMapping("/{tenantId}")
    public ResponseEntity<?> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenant(tenantId)
            .map(tenant -> ResponseEntity.ok(tenant))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{tenantId}")
    public ResponseEntity<?> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantConfiguration tenant) {
        try {
            TenantConfiguration updatedTenant = tenantService.updateTenant(tenantId, tenant);
            return ResponseEntity.ok(updatedTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<?> deleteTenant(@PathVariable String tenantId) {
        try {
            tenantService.deleteTenant(tenantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<?> activateTenant(@PathVariable String tenantId) {
        try {
            tenantService.activateTenant(tenantId);
            return ResponseEntity.ok(Map.of("message", "Tenant activated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error activating tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/{tenantId}/deactivate")
    public ResponseEntity<?> deactivateTenant(@PathVariable String tenantId) {
        try {
            tenantService.deactivateTenant(tenantId);
            return ResponseEntity.ok(Map.of("message", "Tenant deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deactivating tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/{tenantId}/test-connection")
    public ResponseEntity<?> testConnection(@PathVariable String tenantId) {
        try {
            boolean connectionValid = tenantService.testTenantConnection(tenantId);
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("connectionValid", connectionValid);
            response.put("testedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error testing connection for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/{tenantId}/extract")
    public ResponseEntity<?> triggerExtraction(@PathVariable String tenantId) {
        try {
            TenantConfiguration tenant = tenantService.getTenant(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
            
            // Run extraction asynchronously
            new Thread(() -> extractionService.extractEventsForTenant(tenant)).start();
            
            return ResponseEntity.ok(Map.of("message", "Event extraction triggered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error triggering extraction for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/{tenantId}/events")
    public ResponseEntity<Page<ExtractedEvent>> getTenantEvents(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "extractedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ExtractedEvent> events = eventRepository.findAll(pageable);
            
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Error fetching events for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{tenantId}/stats")
    public ResponseEntity<?> getTenantStats(@PathVariable String tenantId) {
        try {
            TenantConfiguration tenant = tenantService.getTenant(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
            
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            Long eventsLast24Hours = eventRepository.countEventsByTenantSince(tenantId, last24Hours);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("tenantId", tenantId);
            stats.put("active", tenant.getActive());
            stats.put("lastSuccessfulExtraction", tenant.getLastSuccessfulExtraction());
            stats.put("lastAttemptedExtraction", tenant.getLastAttemptedExtraction());
            stats.put("lastExtractionError", tenant.getLastExtractionError());
            stats.put("eventsLast24Hours", eventsLast24Hours);
            stats.put("extractionIntervalMinutes", tenant.getExtractionIntervalMinutes());
            
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching stats for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<TenantConfiguration>> getActiveTenants() {
        List<TenantConfiguration> activeTenants = tenantService.getActiveTenants();
        return ResponseEntity.ok(activeTenants);
    }
    
    @GetMapping("/errors")
    public ResponseEntity<List<TenantConfiguration>> getTenantsWithErrors() {
        List<TenantConfiguration> tenantsWithErrors = tenantService.getTenantsWithErrors();
        return ResponseEntity.ok(tenantsWithErrors);
    }
}
