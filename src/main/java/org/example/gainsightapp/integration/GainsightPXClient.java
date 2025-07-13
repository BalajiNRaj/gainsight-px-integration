package org.example.gainsightapp.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

@Service
public class GainsightPXClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public GainsightPXClient(@Value("${gainsight.px.api-url}") String baseUrl, @Value("${gainsight.px.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    // List all users (with optional pageSize and scrollId)
    public ResponseEntity<String> listUsers(Integer pageSize, String scrollId) {
        String url = baseUrl + "/v1/users";

        // Build query parameters if needed
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (pageSize != null) builder.queryParam("pageSize", pageSize);
        if (scrollId != null) builder.queryParam("scrollId", scrollId);

        HttpEntity<Void> request = new HttpEntity<>(createHeaders());

        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, request, String.class);
    }

    // Fetch a user by identifyId
    public ResponseEntity<Map> getUserById(String identifyId) {
        String url = baseUrl + "/v1/users/" + identifyId;
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders()), Map.class);
    }

    // Fetch an account by ID
    public ResponseEntity<Map> getAccountById(String accountId) {
        String url = baseUrl + "/v1/accounts/" + accountId;
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders()), Map.class);
    }

    // Send a custom event
    public ResponseEntity<String> sendCustomEvent(Map<String, Object> payload) {
        String url = baseUrl + "/v1/events/custom";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    // Create headers with auth
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}