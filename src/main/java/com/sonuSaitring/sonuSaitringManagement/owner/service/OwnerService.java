package com.sonuSaitring.sonuSaitringManagement.owner.service;

import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileUpdateRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.VerifyOtpRequest;

public interface OwnerService {

        OwnerRegistrationResponse register(
                        OwnerRegistrationRequest request,
                        MultipartFile logo);

        OwnerLoginResponse login(
                        OwnerLoginRequest request);

        OwnerLoginResponse verifyLoginOtp(
                        VerifyOtpRequest request);

        OwnerProfileResponse getProfile(
                        Long ownerId);

        OwnerProfileResponse updateProfile(
                        Long ownerId,
                        OwnerProfileUpdateRequest request,
                        MultipartFile logo);

        void deleteLogo(
                        Long ownerId);
}