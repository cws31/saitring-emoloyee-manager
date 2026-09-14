package com.sonuSaitring.sonuSaitringManagement.owner.service.Impl;

import com.sonuSaitring.sonuSaitringManagement.common.exception.UnauthorizedException;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerProfileUpdateRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.VerifyOtpRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.owner.otp.OtpChannel;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerLogoService;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerOtpService;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerService;
import com.sonuSaitring.sonuSaitringManagement.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

        private final OwnerRepository ownerRepository;

        private final PasswordEncoder passwordEncoder;

        private final OwnerOtpService ownerOtpService;

        private final OwnerLogoService ownerLogoService;

        private final JwtService jwtService;

        @Override
        @Transactional
        public OwnerRegistrationResponse register(
                        OwnerRegistrationRequest request,
                        MultipartFile logo) {

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                String mobile = request.getMobileNumber()
                                .trim();

                if (ownerRepository.existsByEmail(email)) {

                        throw new UnauthorizedException(
                                        "An owner already exists with this email address.");
                }

                if (ownerRepository.existsByMobileNumber(mobile)) {

                        throw new UnauthorizedException(
                                        "An owner already exists with this mobile number.");
                }

                Owner owner = new Owner();

                owner.setOwnerName(
                                request.getOwnerName().trim());

                owner.setOrganizationName(
                                request.getOrganizationName().trim());

                owner.setEmail(email);

                owner.setMobileNumber(mobile);

                owner.setPassword(
                                passwordEncoder.encode(
                                                request.getPassword()));

                Owner savedOwner = ownerRepository.save(owner);

                if (logo != null && !logo.isEmpty()) {

                        ownerLogoService.saveLogo(
                                        savedOwner.getId(),
                                        logo);
                }

                return new OwnerRegistrationResponse(
                                savedOwner.getId(),
                                savedOwner.getOwnerName(),
                                savedOwner.getOrganizationName(),
                                savedOwner.getEmail(),
                                savedOwner.getMobileNumber(),
                                savedOwner.getLogoUrl());
        }

        @Override
        public OwnerLoginResponse login(
                        OwnerLoginRequest request) {

                String identifier = request.getIdentifier()
                                .trim();

                Owner owner = findOwnerByIdentifier(identifier);

                boolean passwordValid = passwordEncoder.matches(
                                request.getPassword(),
                                owner.getPassword());

                if (!passwordValid) {

                        throw new UnauthorizedException(
                                        "Invalid email/mobile number or password.");
                }

                OtpChannel channel;

                String destination;

                if (isEmail(identifier)) {

                        channel = OtpChannel.EMAIL;

                        destination = owner.getEmail();

                } else {

                        channel = OtpChannel.SMS;

                        destination = owner.getMobileNumber();
                }

                ownerOtpService.createAndSendOtp(
                                owner.getId(),
                                destination,
                                owner.getOwnerName(),
                                channel);

                String message;

                if (channel == OtpChannel.EMAIL) {

                        message = "A verification code has been sent to your registered email address.";

                } else {

                        message = "A verification code has been sent to your registered mobile number.";
                }

                return new OwnerLoginResponse(
                                true,
                                message,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);
        }

        @Override
        public OwnerLoginResponse verifyLoginOtp(
                        VerifyOtpRequest request) {

                String identifier = request.getIdentifier()
                                .trim();

                Owner owner = findOwnerByIdentifier(identifier);

                ownerOtpService.verifyOtp(
                                owner.getId(),
                                request.getOtp());

                String token = jwtService.generateToken(owner);

                return new OwnerLoginResponse(
                                false,
                                "Login successful.",
                                token,
                                "Bearer",
                                owner.getId(),
                                owner.getOwnerName(),
                                owner.getOrganizationName(),
                                owner.getEmail(),
                                owner.getMobileNumber(),
                                owner.getLogoUrl());
        }

        @Override
        @Transactional(readOnly = true)
        public OwnerProfileResponse getProfile(
                        Long ownerId) {

                Owner owner = ownerRepository.findById(ownerId)
                                .orElseThrow(() -> new UnauthorizedException(
                                                "Owner account not found."));

                return toProfileResponse(owner);
        }

        @Override
        @Transactional
        public OwnerProfileResponse updateProfile(
                        Long ownerId,
                        OwnerProfileUpdateRequest request,
                        MultipartFile logo) {

                Owner owner = ownerRepository.findById(ownerId)
                                .orElseThrow(() -> new UnauthorizedException(
                                                "Owner account not found."));

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                String mobile = request.getMobileNumber()
                                .trim();

                if (ownerRepository.existsByEmailAndIdNot(
                                email,
                                ownerId)) {

                        throw new UnauthorizedException(
                                        "An owner already exists with this email address.");
                }

                if (ownerRepository.existsByMobileNumberAndIdNot(
                                mobile,
                                ownerId)) {

                        throw new UnauthorizedException(
                                        "An owner already exists with this mobile number.");
                }

                owner.setOwnerName(
                                request.getOwnerName().trim());

                owner.setOrganizationName(
                                request.getOrganizationName().trim());

                owner.setEmail(email);

                owner.setMobileNumber(mobile);

                owner.setAddressLine1(
                                normalize(request.getAddressLine1()));

                owner.setAddressLine2(
                                normalize(request.getAddressLine2()));

                owner.setCity(
                                normalize(request.getCity()));

                owner.setState(
                                normalize(request.getState()));

                owner.setCountry(
                                normalize(request.getCountry()));

                owner.setPostalCode(
                                normalize(request.getPostalCode()));

                owner.setWebsite(
                                normalize(request.getWebsite()));

                Owner savedOwner = ownerRepository.save(owner);

                if (logo != null && !logo.isEmpty()) {

                        ownerLogoService.saveLogo(
                                        ownerId,
                                        logo);
                }

                return toProfileResponse(savedOwner);
        }

        @Override
        @Transactional
        public void deleteLogo(
                        Long ownerId) {

                if (!ownerRepository.existsById(ownerId)) {

                        throw new UnauthorizedException(
                                        "Owner account not found.");
                }

                ownerLogoService.deleteLogo(ownerId);
        }

        private Owner findOwnerByIdentifier(
                        String identifier) {

                if (isEmail(identifier)) {

                        return ownerRepository
                                        .findByEmail(
                                                        identifier.toLowerCase())
                                        .orElseThrow(
                                                        () -> new UnauthorizedException(
                                                                        "Invalid email/mobile number or password."));
                }

                return ownerRepository
                                .findByMobileNumber(identifier)
                                .orElseThrow(
                                                () -> new UnauthorizedException(
                                                                "Invalid email/mobile number or password."));
        }

        private boolean isEmail(
                        String identifier) {

                return identifier.contains("@");
        }

        private OwnerProfileResponse toProfileResponse(
                        Owner owner) {

                return new OwnerProfileResponse(
                                owner.getId(),
                                owner.getOwnerName(),
                                owner.getOrganizationName(),
                                owner.getEmail(),
                                owner.getMobileNumber(),
                                owner.getAddressLine1(),
                                owner.getAddressLine2(),
                                owner.getCity(),
                                owner.getState(),
                                owner.getCountry(),
                                owner.getPostalCode(),
                                owner.getWebsite(),
                                ownerLogoService.exists(
                                                owner.getId()));
        }

        private String normalize(
                        String value) {

                if (value == null) {

                        return null;
                }

                String trimmed = value.trim();

                return trimmed.isEmpty()
                                ? null
                                : trimmed;
        }
}