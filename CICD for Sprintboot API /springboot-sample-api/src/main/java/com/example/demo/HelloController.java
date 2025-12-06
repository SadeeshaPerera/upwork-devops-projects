package com.example.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final String message;

    public HelloController(@Value("${app.message:Hello from Spring Boot!}") String message) {
        this.message = message;
    }

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", message);
    }
}
