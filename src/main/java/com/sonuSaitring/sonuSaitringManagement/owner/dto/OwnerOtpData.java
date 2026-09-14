package com.sonuSaitring.sonuSaitringManagement.owner.dto;

public record OwnerOtpData(
        String otpHash,
        int attempts) {
}