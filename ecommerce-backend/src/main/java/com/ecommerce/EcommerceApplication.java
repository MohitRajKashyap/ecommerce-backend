package com.ecommerce;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the E-Commerce Backend Application.
 *
 * <p>This is a production-grade, Walmart-level scalable e-commerce backend
 * built using Spring Boot 3.x with JWT security, Redis caching,
 * and a clean layered architecture.</p>
 *
 * @author E-Commerce Engineering Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "E-Commerce Backend API",
                version = "1.0.0",
                description = "Production-grade E-Commerce REST API supporting B2B and B2C workflows. " +
                        "Built with Spring Boot, JWT Auth, Redis, MySQL, and clean layered architecture.",
                contact = @Contact(
                        name = "E-Commerce Engineering Team",
                        email = "dev@ecommerce.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
