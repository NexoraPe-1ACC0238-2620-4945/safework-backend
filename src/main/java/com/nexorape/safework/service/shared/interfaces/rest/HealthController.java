package com.nexorape.safework.service.shared.interfaces.rest;
import org.springframework.web.bind.annotation.*;
@RestController
public class HealthController {
    @GetMapping("/health") public java.util.Map<String,String> health() {return java.util.Map.of("status","UP");}
}
