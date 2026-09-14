package com.sonuSaitring.sonuSaitringManagement.owner.controller;

import com.sonuSaitring.sonuSaitringManagement.common.exception.UnauthorizedException;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileUpdateRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.VerifyOtpRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {

        private final OwnerService ownerService;

        @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<OwnerRegistrationResponse> registerOwner(

                        @Valid @RequestPart("request") OwnerRegistrationRequest request,

                        @RequestPart("logo") MultipartFile logo) {

                OwnerRegistrationResponse response = ownerService.register(
                                request,
                                logo);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @PostMapping("/login")
        public ResponseEntity<OwnerLoginResponse> login(

                        @Valid @RequestBody OwnerLoginRequest request) {

                return ResponseEntity.ok(
                                ownerService.login(request));
        }

        @PostMapping("/verify-otp")
        public ResponseEntity<OwnerLoginResponse> verifyOtp(

                        @Valid @RequestBody VerifyOtpRequest request) {

                return ResponseEntity.ok(
                                ownerService.verifyLoginOtp(request));
        }

        @GetMapping("/profile")
        public ResponseEntity<OwnerProfileResponse> getProfile(
                        Authentication authentication) {

                Long ownerId = getAuthenticatedOwnerId(
                                authentication);

                return ResponseEntity.ok(
                                ownerService.getProfile(
                                                ownerId));
        }

        @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<OwnerProfileResponse> updateProfile(

                        @Valid @RequestPart("request") OwnerProfileUpdateRequest request,

                        @RequestPart(value = "logo", required = false) MultipartFile logo,

                        Authentication authentication) {

                Long ownerId = getAuthenticatedOwnerId(
                                authentication);

                return ResponseEntity.ok(
                                ownerService.updateProfile(
                                                ownerId,
                                                request,
                                                logo));
        }

        @DeleteMapping("/profile/logo")
        public ResponseEntity<Void> deleteLogo(
                        Authentication authentication) {

                Long ownerId = getAuthenticatedOwnerId(
                                authentication);

                ownerService.deleteLogo(
                                ownerId);

                return ResponseEntity
                                .noContent()
                                .build();
        }

        private Long getAuthenticatedOwnerId(
                        Authentication authentication) {

                if (authentication == null ||
                                authentication.getPrincipal() == null) {

                        throw new UnauthorizedException(
                                        "Authentication is required.");
                }

                try {

                        return Long.valueOf(
                                        authentication
                                                        .getPrincipal()
                                                        .toString());

                } catch (NumberFormatException ex) {

                        throw new UnauthorizedException(
                                        "Invalid authenticated owner.");
                }
        }
}