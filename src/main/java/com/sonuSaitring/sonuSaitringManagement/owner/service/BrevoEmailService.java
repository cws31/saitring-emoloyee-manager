package com.sonuSaitring.sonuSaitringManagement.owner.service;

public interface BrevoEmailService {

    void sendLoginOtp(
            String recipientEmail,
            String recipientName,
            String otp);
}