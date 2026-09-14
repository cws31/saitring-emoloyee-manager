package com.sonuSaitring.sonuSaitringManagement.owner.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLogo;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerLogoService;

@RestController
@RequestMapping("/api/owners")
public class OwnerLogoController {

    private final OwnerLogoService ownerLogoService;

    public OwnerLogoController(
            OwnerLogoService ownerLogoService) {

        this.ownerLogoService = ownerLogoService;
    }

    @GetMapping("/{ownerId}/logo")
    public ResponseEntity<byte[]> getLogo(
            @PathVariable Long ownerId) {

        OwnerLogo logo = ownerLogoService.getLogo(ownerId);

        MediaType mediaType = MediaType.parseMediaType(
                logo.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(
                        CacheControl.maxAge(
                                Duration.ofHours(24)).cachePublic())
                .body(logo.getLogoData());
    }
}