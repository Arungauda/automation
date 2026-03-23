package com.invoice.automation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for the Invoice Automation API.
 * This configuration provides comprehensive API documentation with detailed
 * information about all available endpoints, request/response formats,
 * and authentication requirements.
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:Invoice Automation API}")
    private String applicationName;

    /**
     * Configures the OpenAPI specification for the entire application.
     * This includes general API information, servers, and tags organization.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + "/api/v1")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.invoiceautomation.com/v1")
                                .description("Production Server")
                ))
                .tags(List.of(
                        // Invoice Management Tag
                        new Tag()
                                .name("Invoice Management")
                                .description("APIs for managing invoices, items, and PDFs")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Invoice Management Guide")
                                        .url("https://docs.invoiceautomation.com/invoice-guide")),

                        // Invoice Item Management Tag
                        new Tag()
                                .name("Invoice Item Management")
                                .description("APIs for managing invoice items")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Invoice Items Guide")
                                        .url("https://docs.invoiceautomation.com/items-guide")),

                        // File Management Tag
                        new Tag()
                                .name("File Management")
                                .description("APIs for managing file operations")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("File Management Guide")
                                        .url("https://docs.invoiceautomation.com/file-guide")),

                        // Reports and Analytics Tag
                        new Tag()
                                .name("Reports and Analytics")
                                .description("APIs for generating reports and analytics")
                                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                                        .description("Reports Guide")
                                        .url("https://docs.invoiceautomation.com/reports-guide")),

                        // General Operations Tag
                        new Tag()
                                .name("General Operations")
                                .description("General system operations and health checks")
                ));
    }

    /**
     * Provides general information about the API.
     */
    private Info apiInfo() {
        return new Info()
                .title("Invoice Automation API")
                .description("A comprehensive REST API for invoice management, file processing, and analytics. " +
                        "This API provides endpoints for creating, updating, and managing invoices, " +
                        "handling invoice items, file uploads/downloads, and generating detailed reports.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Invoice Automation Team")
                        .email("support@invoiceautomation.com")
                        .url("https://www.invoiceautomation.com/contact"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"))
                .termsOfService("https://www.invoiceautomation.com/terms");
    }
}
