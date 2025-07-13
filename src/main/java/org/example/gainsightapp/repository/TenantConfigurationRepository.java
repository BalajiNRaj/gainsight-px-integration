package org.example.gainsightapp.repository;

import org.example.gainsightapp.model.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, Long> {
    
    Optional<TenantConfiguration> findByTenantId(String tenantId);
    
    List<TenantConfiguration> findByActiveTrue();
    
    @Query("SELECT t FROM TenantConfiguration t WHERE t.active = true AND " +
           "(t.lastAttemptedExtraction IS NULL OR " +
           "t.lastAttemptedExtraction < :cutoffTime)")
    List<TenantConfiguration> findTenantsReadyForExtraction(LocalDateTime cutoffTime);
    
    @Query("SELECT t FROM TenantConfiguration t WHERE t.active = true AND " +
           "t.lastExtractionError IS NOT NULL")
    List<TenantConfiguration> findTenantsWithErrors();
}
