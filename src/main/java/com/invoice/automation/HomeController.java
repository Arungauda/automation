package com.invoice.automation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home controller providing basic application endpoints.
 * Includes health check and application status endpoints.
 */
@RestController
@Tag(name = "General Operations", description = "General system operations and health checks")
public class HomeController {

    @Operation(summary = "Application home", description = "Returns a welcome message indicating the application is running")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Welcome message returned successfully")
    })
    @GetMapping("/")
    public String home() {
        return "Invoice Automation Application is running!";
    }

    @Operation(summary = "Health check", description = "Health check endpoint for monitoring application status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is healthy"),
            @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    })
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
