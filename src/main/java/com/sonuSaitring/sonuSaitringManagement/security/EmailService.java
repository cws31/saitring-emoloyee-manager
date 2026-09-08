package com.sonuSaitring.sonuSaitringManagement.security;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
}