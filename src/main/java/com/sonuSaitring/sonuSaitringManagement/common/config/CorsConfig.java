package com.sonuSaitring.sonuSaitringManagement.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        logger.info(
                "CORS configuration initialized: frontendOrigin={}",
                frontendUrl);

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(frontendUrl)
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "PATCH",
                                "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                logger.info(
                        "CORS mappings registered: origin={}, credentialsEnabled=true",
                        frontendUrl);
            }
        };
    }
}