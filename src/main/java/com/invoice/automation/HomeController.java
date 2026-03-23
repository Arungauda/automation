package com.invoice.automation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Invoice Automation Application is running!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
