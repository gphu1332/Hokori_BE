package com.hokori.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiDocsController {

    @GetMapping("/v3/api-docs")
    public Map<String, Object> apiDocs() {
        Map<String, Object> openApi = new HashMap<>();
        openApi.put("openapi", "3.0.1");
        openApi.put("info", Map.of(
            "title", "Hokori API",
            "version", "1.0.0",
            "description", "Japanese Learning Platform API"
        ));
        openApi.put("servers", new Object[]{
            Map.of("url", "https://web-production-521b5.up.railway.app", "description", "Production server")
        });
        openApi.put("paths", new HashMap<>());
        return openApi;
    }

    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String, Object> swaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "/v3/api-docs");
        config.put("validatorUrl", "");
        return config;
    }
}
