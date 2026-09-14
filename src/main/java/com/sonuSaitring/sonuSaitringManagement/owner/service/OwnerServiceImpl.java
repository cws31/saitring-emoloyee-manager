package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ConflictException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.UnauthorizedException;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerLoginResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.OwnerRegistrationResponse;
import com.sonuSaitring.sonuSaitringManagement.owner.dto.VerifyOtpRequest;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLoginOtp;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerLoginOtpRepository;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;
import com.sonuSaitring.sonuSaitringManagement.security.JwtService;

@Service
public class OwnerServiceImpl implements OwnerService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final JwtService jwtService;
    private final OwnerLoginOtpRepository otpRepository;
    private final BrevoEmailService brevoEmailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public OwnerServiceImpl(
            OwnerRepository ownerRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService,
            JwtService jwtService,
            OwnerLoginOtpRepository otpRepository,
            BrevoEmailService brevoEmailService) {

        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.jwtService = jwtService;
        this.otpRepository = otpRepository;
        this.brevoEmailService = brevoEmailService;
    }

  

    @Override
    public OwnerRegistrationResponse register(
            OwnerRegistrationRequest request,
            MultipartFile logo) {

        if (ownerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Email is already registered.");
        }

        if (ownerRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException(
                    "Username is already taken.");
        }

        String logoUrl = fileStorageService.uploadOrganizationLogo(logo);

        Owner owner = new Owner();

        owner.setOwnerName(
                request.getOwnerName().trim());

        owner.setOrganizationName(
                request.getOrganizationName().trim());

        owner.setEmail(
                request.getEmail().trim().toLowerCase());

        owner.setUsername(
                request.getUsername().trim());

        owner.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        owner.setLogoUrl(logoUrl);

        Owner savedOwner = ownerRepository.save(owner);

        return new OwnerRegistrationResponse(
                savedOwner.getId(),
                savedOwner.getOwnerName(),
                savedOwner.getOrganizationName(),
                savedOwner.getEmail(),
                savedOwner.getUsername(),
                savedOwner.getLogoUrl());
    }

    @Override
    @Transactional
    public OwnerLoginResponse login(
            OwnerLoginRequest request) {

        Owner owner = ownerRepository
                .findByUsername(
                        request.getUsername().trim())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid username or password."));

       
        if (!passwordEncoder.matches(
                request.getPassword(),
                owner.getPassword())) {

            throw new UnauthorizedException(
                    "Invalid username or password.");
        }

        otpRepository
                .findTopByOwnerIdAndUsedFalseOrderByCreatedAtDesc(
                        owner.getId())
                .ifPresent(previousOtp -> {

                    previousOtp.setUsed(true);

                    otpRepository.save(previousOtp);
                });

      
        String otp = generateOtp();

      
        OwnerLoginOtp loginOtp = new OwnerLoginOtp();

        loginOtp.setOwner(owner);

        loginOtp.setOtpHash(
                passwordEncoder.encode(otp));

        loginOtp.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(OTP_EXPIRY_MINUTES));

        loginOtp.setUsed(false);

        loginOtp.setAttempts(0);

        loginOtp.setCreatedAt(
                LocalDateTime.now());

        otpRepository.save(loginOtp);

     
        brevoEmailService.sendLoginOtp(
                owner.getEmail(),
                owner.getOwnerName(),
                otp);

  
        return new OwnerLoginResponse(
                true,
                "A verification code has been sent to your registered email address.",
                null,
                null,
                null,
                null,
                null,
                owner.getUsername(),
                null);
    }

 

    @Override
    @Transactional
    public OwnerLoginResponse verifyLoginOtp(
            VerifyOtpRequest request) {

        Owner owner = ownerRepository
                .findByUsername(
                        request.getUsername().trim())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid verification request."));

      
        OwnerLoginOtp loginOtp = otpRepository
                .findTopByOwnerIdAndUsedFalseOrderByCreatedAtDesc(
                        owner.getId())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid or expired verification code."));

        if (loginOtp.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

           
            loginOtp.setUsed(true);

            otpRepository.save(loginOtp);

            throw new UnauthorizedException(
                    "Verification code has expired. Please request a new code.");
        }

      
        if (loginOtp.getAttempts() >= MAX_OTP_ATTEMPTS) {

            loginOtp.setUsed(true);

            otpRepository.save(loginOtp);

            throw new UnauthorizedException(
                    "Too many incorrect attempts. Please request a new code.");
        }

       
        boolean otpMatches = passwordEncoder.matches(
                request.getOtp(),
                loginOtp.getOtpHash());

     
        if (!otpMatches) {

           
            int attempts = loginOtp.getAttempts() + 1;

            loginOtp.setAttempts(attempts);

            if (attempts >= MAX_OTP_ATTEMPTS) {

                loginOtp.setUsed(true);

                otpRepository.save(loginOtp);

                throw new UnauthorizedException(
                        "Too many incorrect attempts. Please request a new code.");
            }

         
            otpRepository.save(loginOtp);

            throw new UnauthorizedException(
                    "Invalid verification code.");
        }

       
        loginOtp.setUsed(true);

        otpRepository.save(loginOtp);

        String token = jwtService.generateToken(owner);

        return new OwnerLoginResponse(
                false,
                "Login successful.",
                token,
                "Bearer",
                owner.getId(),
                owner.getOwnerName(),
                owner.getOrganizationName(),
                owner.getUsername(),
                owner.getLogoUrl());
    }



    private String generateOtp() {

        int otp = 100000
                + secureRandom.nextInt(900000);

        return String.valueOf(otp);
    }
}