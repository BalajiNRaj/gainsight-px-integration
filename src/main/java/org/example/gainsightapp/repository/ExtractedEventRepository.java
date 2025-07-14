package org.example.gainsightapp.repository;

import org.example.gainsightapp.model.ExtractedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExtractedEventRepository extends MongoRepository<ExtractedEvent, String> {
    
    boolean existsByTenantIdAndEventId(String tenantId, String eventId);
    
    List<ExtractedEvent> findByTenantIdAndStatus(String tenantId, ExtractedEvent.ProcessingStatus status);
    
    @Query("{ 'status': ?0, 'retryCount': { $lt: ?1 } }")
    List<ExtractedEvent> findFailedEventsForRetry(ExtractedEvent.ProcessingStatus status, Integer maxRetries);
    
    @Query(value = "{ 'tenantId': ?0, 'extractedAt': { $gt: ?1 } }", count = true)
    Long countEventsByTenantSince(String tenantId, LocalDateTime since);
    
    Optional<ExtractedEvent> findTopByTenantIdOrderByEventTimestampDesc(String tenantId);
}
