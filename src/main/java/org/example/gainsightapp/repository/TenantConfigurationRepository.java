package org.example.gainsightapp.repository;

import org.example.gainsightapp.model.TenantConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantConfigurationRepository extends MongoRepository<TenantConfiguration, String> {
    
    Optional<TenantConfiguration> findByTenantId(String tenantId);
    
    List<TenantConfiguration> findByActiveTrue();
    
    @Query("{ 'active': true, $or: [ { 'lastAttemptedExtraction': null }, { 'lastAttemptedExtraction': { $lt: ?0 } } ] }")
    List<TenantConfiguration> findTenantsReadyForExtraction(LocalDateTime cutoffTime);
    
    @Query("{ 'active': true, 'lastExtractionError': { $ne: null } }")
    List<TenantConfiguration> findTenantsWithErrors();
}
