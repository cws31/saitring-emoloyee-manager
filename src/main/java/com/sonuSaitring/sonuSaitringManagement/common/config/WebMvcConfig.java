package com.sonuSaitring.sonuSaitringManagement.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${file.storage.location:uploads}")
    private String storageLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadDir = Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();

        String uploadPath = uploadDir.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath + "/");

        logger.info(
                "Static upload resource mapping initialized: resource=/uploads/**, storageLocation={}",
                uploadDir);
    }
}