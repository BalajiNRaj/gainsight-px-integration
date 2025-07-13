package org.example.gainsightapp.repository;

import org.example.gainsightapp.model.ExtractedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExtractedEventRepository extends JpaRepository<ExtractedEvent, Long> {
    
    boolean existsByTenantIdAndEventId(String tenantId, String eventId);
    
    List<ExtractedEvent> findByTenantIdAndStatus(String tenantId, ExtractedEvent.ProcessingStatus status);
    
    @Query("SELECT e FROM ExtractedEvent e WHERE e.status = :status AND e.retryCount < :maxRetries")
    List<ExtractedEvent> findFailedEventsForRetry(ExtractedEvent.ProcessingStatus status, Integer maxRetries);
    
    @Query("SELECT COUNT(e) FROM ExtractedEvent e WHERE e.tenantId = :tenantId AND e.extractedAt > :since")
    Long countEventsByTenantSince(String tenantId, LocalDateTime since);
    
    Optional<ExtractedEvent> findTopByTenantIdOrderByEventTimestampDesc(String tenantId);
}
