package com.sonuSaitring.sonuSaitringManagement.owner.otp;

public record OwnerOtpData(
        String otpHash,
        int attempts,
        OtpChannel channel) {
}