package org.example.gainsightapp.controller;

import org.example.gainsightapp.integration.GainsightPXClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gainsight")
public class GainsightPXController {

    private final GainsightPXClient pxClient;

    public GainsightPXController(GainsightPXClient pxClient) {
        this.pxClient = pxClient;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String scrollId
    ) {
        return pxClient.listUsers(pageSize, scrollId);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        return pxClient.getUserById(id);
    }

    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        return pxClient.getAccountById(id);
    }

    @PostMapping("/event")
    public ResponseEntity<?> postEvent(@RequestBody Map<String, Object> eventPayload) {
        return pxClient.sendCustomEvent(eventPayload);
    }
}