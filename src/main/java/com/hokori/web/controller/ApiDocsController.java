package com.hokori.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiDocsController {

    @Value("${server.port:8080}")
    private String serverPort;

    // Removed manual /v3/api-docs endpoint to let SpringDoc handle it automatically

    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String, Object> swaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "/v3/api-docs");
        config.put("validatorUrl", "");
        return config;
    }
}
