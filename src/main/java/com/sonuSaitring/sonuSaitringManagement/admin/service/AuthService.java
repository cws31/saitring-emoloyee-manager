package com.sonuSaitring.sonuSaitringManagement.admin.service;

public interface AuthService {
    void initiateLogin(String username, String password);

    String verifyOtpAndGenerateToken(String username, String otp);
}