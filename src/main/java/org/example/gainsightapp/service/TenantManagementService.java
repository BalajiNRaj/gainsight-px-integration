package org.example.gainsightapp.service;

import org.example.gainsightapp.integration.MultiTenantGainsightPXClient;
import org.example.gainsightapp.model.TenantConfiguration;
import org.example.gainsightapp.repository.TenantConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TenantManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantManagementService.class);
    
    private final TenantConfigurationRepository tenantRepository;
    private final MultiTenantGainsightPXClient gainsightClient;
    
    public TenantManagementService(
            TenantConfigurationRepository tenantRepository,
            MultiTenantGainsightPXClient gainsightClient) {
        this.tenantRepository = tenantRepository;
        this.gainsightClient = gainsightClient;
    }
    
    @Transactional
    public TenantConfiguration createTenant(TenantConfiguration tenant) {
        logger.info("Creating new tenant configuration: {}", tenant.getTenantId());
        
        // Validate tenant doesn't already exist
        if (tenantRepository.findByTenantId(tenant.getTenantId()).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists: " + tenant.getTenantId());
        }
        
        // Test connection before saving
        if (!gainsightClient.testConnection(tenant)) {
            throw new IllegalArgumentException("Invalid Gainsight PX credentials for tenant: " + tenant.getTenantId());
        }
        
        TenantConfiguration savedTenant = tenantRepository.save(tenant);
        logger.info("Successfully created tenant configuration: {}", savedTenant.getTenantId());
        
        return savedTenant;
    }
    
    @Transactional
    public TenantConfiguration updateTenant(String tenantId, TenantConfiguration updatedTenant) {
        logger.info("Updating tenant configuration: {}", tenantId);
        
        TenantConfiguration existingTenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        // Update fields
        existingTenant.setCompanyName(updatedTenant.getCompanyName());
        existingTenant.setApiKey(updatedTenant.getApiKey());
        existingTenant.setApiUrl(updatedTenant.getApiUrl());
        existingTenant.setActive(updatedTenant.getActive());
        existingTenant.setExtractionIntervalMinutes(updatedTenant.getExtractionIntervalMinutes());
        existingTenant.setExtractCustomEvents(updatedTenant.getExtractCustomEvents());
        existingTenant.setExtractStandardEvents(updatedTenant.getExtractStandardEvents());
        existingTenant.setMaxRetryAttempts(updatedTenant.getMaxRetryAttempts());
        existingTenant.setTimeoutSeconds(updatedTenant.getTimeoutSeconds());
        
        // Test connection if credentials changed
        if (!existingTenant.getApiKey().equals(updatedTenant.getApiKey()) || 
            !existingTenant.getApiUrl().equals(updatedTenant.getApiUrl())) {
            if (!gainsightClient.testConnection(existingTenant)) {
                throw new IllegalArgumentException("Invalid Gainsight PX credentials for tenant: " + tenantId);
            }
        }
        
        TenantConfiguration savedTenant = tenantRepository.save(existingTenant);
        logger.info("Successfully updated tenant configuration: {}", savedTenant.getTenantId());
        
        return savedTenant;
    }
    
    @Transactional
    public void deleteTenant(String tenantId) {
        logger.info("Deleting tenant configuration: {}", tenantId);
        
        TenantConfiguration tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        tenantRepository.delete(tenant);
        logger.info("Successfully deleted tenant configuration: {}", tenantId);
    }
    
    @Transactional
    public void deactivateTenant(String tenantId) {
        logger.info("Deactivating tenant: {}", tenantId);
        
        TenantConfiguration tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        tenant.setActive(false);
        tenantRepository.save(tenant);
        
        logger.info("Successfully deactivated tenant: {}", tenantId);
    }
    
    @Transactional
    public void activateTenant(String tenantId) {
        logger.info("Activating tenant: {}", tenantId);
        
        TenantConfiguration tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        // Test connection before activating
        if (!gainsightClient.testConnection(tenant)) {
            throw new IllegalArgumentException("Cannot activate tenant with invalid credentials: " + tenantId);
        }
        
        tenant.setActive(true);
        tenant.setLastExtractionError(null); // Clear any previous errors
        tenantRepository.save(tenant);
        
        logger.info("Successfully activated tenant: {}", tenantId);
    }
    
    public Optional<TenantConfiguration> getTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId);
    }
    
    public List<TenantConfiguration> getAllTenants() {
        return tenantRepository.findAll();
    }
    
    public List<TenantConfiguration> getActiveTenants() {
        return tenantRepository.findByActiveTrue();
    }
    
    public List<TenantConfiguration> getTenantsWithErrors() {
        return tenantRepository.findTenantsWithErrors();
    }
    
    public List<TenantConfiguration> getTenantsReadyForExtraction() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5); // 5 minutes ago
        return tenantRepository.findTenantsReadyForExtraction(cutoffTime);
    }
    
    public boolean testTenantConnection(String tenantId) {
        TenantConfiguration tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        return gainsightClient.testConnection(tenant);
    }
}
