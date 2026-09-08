package com.sonuSaitring.sonuSaitringManagement.admin.controller;

import com.sonuSaitring.sonuSaitringManagement.admin.dto.LoginRequest;
import com.sonuSaitring.sonuSaitringManagement.admin.dto.OtpVerificationRequest;
import com.sonuSaitring.sonuSaitringManagement.admin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        authService.initiateLogin(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("OTP has been sent to your registered email address.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpVerificationRequest request) {
        String token = authService.verifyOtpAndGenerateToken(request.getUsername(), request.getOtp());
        return ResponseEntity.ok("Bearer " + token);
    }
}