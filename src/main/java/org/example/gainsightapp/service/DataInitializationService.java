package org.example.gainsightapp.service;

import org.example.gainsightapp.model.TenantConfiguration;
import org.example.gainsightapp.repository.TenantConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class DataInitializationService implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    private final TenantConfigurationRepository tenantRepository;
    private final String defaultApiUrl;
    private final String defaultApiKey;
    
    public DataInitializationService(
            TenantConfigurationRepository tenantRepository,
            @Value("${gainsight.px.api-url}") String defaultApiUrl,
            @Value("${gainsight.px.api-key}") String defaultApiKey) {
        this.tenantRepository = tenantRepository;
        this.defaultApiUrl = defaultApiUrl;
        this.defaultApiKey = defaultApiKey;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Initializing sample tenant configurations");
        
        // Create sample tenant configurations if none exist
        if (tenantRepository.count() == 0) {
            createSampleTenants();
        }
        
        logger.info("Data initialization completed. Total tenants: {}", tenantRepository.count());
    }
    
    private void createSampleTenants() {
        // Sample tenant 1
        TenantConfiguration tenant1 = new TenantConfiguration(
            "tenant-001",
            "Acme Corporation",
            defaultApiKey,
            defaultApiUrl
        );
        tenant1.setExtractionIntervalMinutes(5);
        tenant1.setExtractCustomEvents(true);
        tenant1.setExtractStandardEvents(true);
        tenant1.setActive(true);
        
        // Sample tenant 2
        TenantConfiguration tenant2 = new TenantConfiguration(
            "tenant-002",
            "TechStart Inc",
            defaultApiKey,
            defaultApiUrl
        );
        tenant2.setExtractionIntervalMinutes(10);
        tenant2.setExtractCustomEvents(true);
        tenant2.setExtractStandardEvents(false);
        tenant2.setActive(false); // Inactive for demo
        
        // Sample tenant 3
        TenantConfiguration tenant3 = new TenantConfiguration(
            "tenant-003",
            "Global Enterprise Ltd",
            defaultApiKey,
            defaultApiUrl
        );
        tenant3.setExtractionIntervalMinutes(3);
        tenant3.setExtractCustomEvents(false);
        tenant3.setExtractStandardEvents(true);
        tenant3.setActive(true);
        
        tenantRepository.save(tenant1);
        tenantRepository.save(tenant2);
        tenantRepository.save(tenant3);
        
        logger.info("Created {} sample tenant configurations", 3);
    }
}
