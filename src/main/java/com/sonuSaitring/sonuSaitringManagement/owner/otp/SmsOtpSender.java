package com.sonuSaitring.sonuSaitringManagement.owner.otp;

public interface SmsOtpSender {

    void sendLoginOtp(
            String recipientMobile,
            String recipientName,
            String otp);
}