package com.sonuSaitring.sonuSaitringManagement.admin.dto;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String username;
    private String otp;
}