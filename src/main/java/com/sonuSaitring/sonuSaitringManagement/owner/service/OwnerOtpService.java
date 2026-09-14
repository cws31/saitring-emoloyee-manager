package com.sonuSaitring.sonuSaitringManagement.owner.service;

public interface OwnerOtpService {

    void createAndSendOtp(
            Long ownerId,
            String email,
            String ownerName);

    void verifyOtp(
            Long ownerId,
            String otp);

    void invalidateOtp(Long ownerId);
}