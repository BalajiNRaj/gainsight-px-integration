package org.example.gainsightapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GainsightScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(GainsightScheduledTaskService.class);
    
    private final GainsightEventExtractionService extractionService;
    
    public GainsightScheduledTaskService(GainsightEventExtractionService extractionService) {
        this.extractionService = extractionService;
    }
    
    /**
     * Scheduled task that runs every 5 minutes to extract events from all active tenants
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes = 300,000 milliseconds
    public void scheduledEventExtraction() {
        logger.info("Starting scheduled event extraction task");
        
        try {
            extractionService.extractEventsForAllTenants();
            logger.info("Completed scheduled event extraction task successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled event extraction: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Backup scheduled task that runs every hour to handle any missed extractions
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour = 3,600,000 milliseconds
    public void hourlyBackupExtraction() {
        logger.info("Starting hourly backup extraction task");
        
        try {
            extractionService.extractEventsForAllTenants();
            logger.info("Completed hourly backup extraction task successfully");
        } catch (Exception e) {
            logger.error("Error during hourly backup extraction: {}", e.getMessage(), e);
        }
    }
}
