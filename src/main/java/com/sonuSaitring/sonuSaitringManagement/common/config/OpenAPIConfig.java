package com.sonuSaitring.sonuSaitringManagement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Saitring Employee Manager System API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing employees, attendance, advances, " +
                                "settlements, monthly closings and administrative operations.\n\n" +
                                "Built by Sonu Kumar."
                        )
                        .contact(new Contact()
                                .name("Sonu Kumar")
                                .url("https://www.linkedin.com/in/sonu-kumar-9a59b52a4/")
                                .email("gautamrocky909621@gmail.com")
                        ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}