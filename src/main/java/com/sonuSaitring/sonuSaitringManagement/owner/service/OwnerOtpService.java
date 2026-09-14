package com.sonuSaitring.sonuSaitringManagement.owner.service;

import com.sonuSaitring.sonuSaitringManagement.owner.otp.OtpChannel;

public interface OwnerOtpService {

    void createAndSendOtp(
            Long ownerId,
            String destination,
            String ownerName,
            OtpChannel channel);

    void verifyOtp(
            Long ownerId,
            String otp);

    void invalidateOtp(
            Long ownerId);
}