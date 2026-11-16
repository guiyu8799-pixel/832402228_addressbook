package com.fzu.addressbook.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Small liveness endpoint used by the start script to wait for the API before opening the browser.
 */
@RestController
public class HealthController {

    /**
     * Reports that the application is up.
     *
     * @return status, application name and server time
     */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "addressbook");
        body.put("time", LocalDateTime.now().toString());
        return body;
    }
}
