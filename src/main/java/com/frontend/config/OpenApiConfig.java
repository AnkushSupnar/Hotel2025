package com.frontend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for Hotel Management API
 * Only active in 'server' profile
 */
@Configuration
@Profile("server")
public class OpenApiConfig {

    @Bean
    public OpenAPI hotelManagementOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8081");
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Hotel Management System");
        contact.setEmail("support@hotel.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Hotel Management System API")
                .version("1.0.0")
                .description("REST API for Hotel Management System - Billing, Tables, Orders, Customers, Items, and more")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
