package com.sonuSaitring.sonuSaitringManagement.owner.controller;

import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.VerifyOtpRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
}