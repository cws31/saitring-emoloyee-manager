package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OwnerServiceImpl implements OwnerService {

        private static final Logger log = LoggerFactory.getLogger(OwnerServiceImpl.class);

        private static final int OTP_EXPIRY_MINUTES = 5;
        private static final int MAX_OTP_ATTEMPTS = 5;

        private final OwnerRepository ownerRepository;
        private final PasswordEncoder passwordEncoder;
        private final FileStorageService fileStorageService;
        private final JwtService jwtService;
        private final OwnerLoginOtpRepository otpRepository;
        private final BrevoEmailService brevoEmailService;

        private final SecureRandom secureRandom = new SecureRandom();

        private final Counter registrationSuccessCounter;
        private final Counter registrationFailureCounter;

        private final Counter loginSuccessCounter;
        private final Counter loginFailureCounter;

        private final Counter otpCreatedCounter;
        private final Counter otpSuccessCounter;
        private final Counter otpInvalidCounter;
        private final Counter otpExpiredCounter;
        private final Counter otpMaxAttemptsCounter;

        private final Timer registrationTimer;
        private final Timer loginTimer;
        private final Timer otpVerificationTimer;

        public OwnerServiceImpl(
                        OwnerRepository ownerRepository,
                        PasswordEncoder passwordEncoder,
                        FileStorageService fileStorageService,
                        JwtService jwtService,
                        OwnerLoginOtpRepository otpRepository,
                        BrevoEmailService brevoEmailService,
                        MeterRegistry meterRegistry) {

                this.ownerRepository = ownerRepository;
                this.passwordEncoder = passwordEncoder;
                this.fileStorageService = fileStorageService;
                this.jwtService = jwtService;
                this.otpRepository = otpRepository;
                this.brevoEmailService = brevoEmailService;

                this.registrationSuccessCounter = Counter.builder(
                                "owner.registration.success")
                                .description("Successful owner registrations")
                                .register(meterRegistry);

                this.registrationFailureCounter = Counter.builder(
                                "owner.registration.failure")
                                .description("Failed owner registrations")
                                .register(meterRegistry);

                this.loginSuccessCounter = Counter.builder(
                                "owner.login.success")
                                .description("Successful owner login flows")
                                .register(meterRegistry);

                this.loginFailureCounter = Counter.builder(
                                "owner.login.failure")
                                .description("Failed owner login attempts")
                                .register(meterRegistry);

                this.otpCreatedCounter = Counter.builder(
                                "owner.otp.created")
                                .description("Login OTPs created")
                                .register(meterRegistry);

                this.otpSuccessCounter = Counter.builder(
                                "owner.otp.verification.success")
                                .description("Successful OTP verifications")
                                .register(meterRegistry);

                this.otpInvalidCounter = Counter.builder(
                                "owner.otp.verification.invalid")
                                .description("Invalid OTP verification attempts")
                                .register(meterRegistry);

                this.otpExpiredCounter = Counter.builder(
                                "owner.otp.verification.expired")
                                .description("Expired OTP verification attempts")
                                .register(meterRegistry);

                this.otpMaxAttemptsCounter = Counter.builder(
                                "owner.otp.verification.max_attempts")
                                .description("OTP verifications blocked because maximum attempts were reached")
                                .register(meterRegistry);

                this.registrationTimer = Timer.builder(
                                "owner.registration.duration")
                                .description("Owner registration duration")
                                .register(meterRegistry);

                this.loginTimer = Timer.builder(
                                "owner.login.duration")
                                .description("Owner login duration")
                                .register(meterRegistry);

                this.otpVerificationTimer = Timer.builder(
                                "owner.otp.verification.duration")
                                .description("OTP verification duration")
                                .register(meterRegistry);
        }

        @Override
        public OwnerRegistrationResponse register(
                        OwnerRegistrationRequest request,
                        MultipartFile logo) {

                long start = System.nanoTime();

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                String username = request.getUsername()
                                .trim();

                log.info(
                                "Owner registration started username={} email={}",
                                username,
                                maskEmail(email));

                try {

                        if (ownerRepository.existsByEmail(email)) {

                                registrationFailureCounter.increment();

                                log.warn(
                                                "Owner registration rejected because email already exists email={}",
                                                maskEmail(email));

                                throw new ConflictException(
                                                "Email is already registered.");
                        }

                        if (ownerRepository.existsByUsername(username)) {

                                registrationFailureCounter.increment();

                                log.warn(
                                                "Owner registration rejected because username already exists username={}",
                                                username);

                                throw new ConflictException(
                                                "Username is already taken.");
                        }

                        String logoUrl = fileStorageService.uploadOrganizationLogo(logo);

                        Owner owner = new Owner();

                        owner.setOwnerName(
                                        request.getOwnerName().trim());

                        owner.setOrganizationName(
                                        request.getOrganizationName().trim());

                        owner.setEmail(email);

                        owner.setUsername(username);

                        owner.setPassword(
                                        passwordEncoder.encode(
                                                        request.getPassword()));

                        owner.setLogoUrl(logoUrl);

                        Owner savedOwner = ownerRepository.save(owner);

                        registrationSuccessCounter.increment();

                        log.info(
                                        "Owner registration completed successfully ownerId={} username={}",
                                        savedOwner.getId(),
                                        savedOwner.getUsername());

                        return new OwnerRegistrationResponse(
                                        savedOwner.getId(),
                                        savedOwner.getOwnerName(),
                                        savedOwner.getOrganizationName(),
                                        savedOwner.getEmail(),
                                        savedOwner.getUsername(),
                                        savedOwner.getLogoUrl());

                } catch (ConflictException ex) {

                        throw ex;

                } catch (Exception ex) {

                        registrationFailureCounter.increment();

                        log.error(
                                        "Owner registration failed username={} errorType={}",
                                        username,
                                        ex.getClass().getSimpleName(),
                                        ex);

                        throw ex;

                } finally {

                        registrationTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
        }

        @Override
        @Transactional
        public OwnerLoginResponse login(
                        OwnerLoginRequest request) {

                long start = System.nanoTime();

                String username = request.getUsername().trim();

                log.info(
                                "Owner login started username={}",
                                username);

                try {

                        Owner owner = ownerRepository
                                        .findByUsername(username)
                                        .orElseThrow(() -> {

                                                loginFailureCounter.increment();

                                                log.warn(
                                                                "Owner login failed because username was not found username={}",
                                                                username);

                                                return new UnauthorizedException(
                                                                "Invalid username or password.");
                                        });

                        if (!passwordEncoder.matches(
                                        request.getPassword(),
                                        owner.getPassword())) {

                                loginFailureCounter.increment();

                                log.warn(
                                                "Owner login failed because password was invalid username={}",
                                                username);

                                throw new UnauthorizedException(
                                                "Invalid username or password.");
                        }

                        otpRepository
                                        .findTopByOwnerIdAndUsedFalseOrderByCreatedAtDesc(
                                                        owner.getId())
                                        .ifPresent(previousOtp -> {

                                                previousOtp.setUsed(true);

                                                otpRepository.save(previousOtp);

                                                log.debug(
                                                                "Previous unused OTP invalidated ownerId={}",
                                                                owner.getId());
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

                        otpCreatedCounter.increment();

                        log.info(
                                        "Login OTP created ownerId={} expiresInMinutes={}",
                                        owner.getId(),
                                        OTP_EXPIRY_MINUTES);

                        /*
                         * IMPORTANT:
                         * Never log the actual OTP.
                         */
                        brevoEmailService.sendLoginOtp(
                                        owner.getEmail(),
                                        owner.getOwnerName(),
                                        otp);

                        log.info(
                                        "Login OTP delivery requested ownerId={} email={}",
                                        owner.getId(),
                                        maskEmail(owner.getEmail()));

                        loginSuccessCounter.increment();

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

                } catch (UnauthorizedException ex) {

                        throw ex;

                } catch (Exception ex) {

                        loginFailureCounter.increment();

                        log.error(
                                        "Owner login failed unexpectedly username={} errorType={}",
                                        username,
                                        ex.getClass().getSimpleName(),
                                        ex);

                        throw ex;

                } finally {

                        loginTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
        }

        @Override
        @Transactional
        public OwnerLoginResponse verifyLoginOtp(
                        VerifyOtpRequest request) {

                long start = System.nanoTime();

                String username = request.getUsername().trim();

                log.info(
                                "OTP verification started username={}",
                                username);

                try {

                        Owner owner = ownerRepository
                                        .findByUsername(username)
                                        .orElseThrow(() -> {

                                                loginFailureCounter.increment();

                                                log.warn(
                                                                "OTP verification rejected because username was not found username={}",
                                                                username);

                                                return new UnauthorizedException(
                                                                "Invalid verification request.");
                                        });

                        OwnerLoginOtp loginOtp = otpRepository
                                        .findTopByOwnerIdAndUsedFalseOrderByCreatedAtDesc(
                                                        owner.getId())
                                        .orElseThrow(() -> {

                                                otpInvalidCounter.increment();

                                                log.warn(
                                                                "OTP verification failed because no active OTP exists ownerId={}",
                                                                owner.getId());

                                                return new UnauthorizedException(
                                                                "Invalid or expired verification code.");
                                        });

                        if (loginOtp.getExpiresAt()
                                        .isBefore(LocalDateTime.now())) {

                                loginOtp.setUsed(true);

                                otpRepository.save(loginOtp);

                                otpExpiredCounter.increment();

                                log.warn(
                                                "OTP verification failed because OTP expired ownerId={}",
                                                owner.getId());

                                throw new UnauthorizedException(
                                                "Verification code has expired. Please request a new code.");
                        }

                        if (loginOtp.getAttempts() >= MAX_OTP_ATTEMPTS) {

                                loginOtp.setUsed(true);

                                otpRepository.save(loginOtp);

                                otpMaxAttemptsCounter.increment();

                                log.warn(
                                                "OTP verification blocked because maximum attempts were reached ownerId={}",
                                                owner.getId());

                                throw new UnauthorizedException(
                                                "Too many incorrect attempts. Please request a new code.");
                        }

                        boolean otpMatches = passwordEncoder.matches(
                                        request.getOtp(),
                                        loginOtp.getOtpHash());

                        if (!otpMatches) {

                                int attempts = loginOtp.getAttempts() + 1;

                                loginOtp.setAttempts(attempts);

                                otpInvalidCounter.increment();

                                if (attempts >= MAX_OTP_ATTEMPTS) {

                                        loginOtp.setUsed(true);

                                        otpRepository.save(loginOtp);

                                        otpMaxAttemptsCounter.increment();

                                        log.warn(
                                                        "OTP verification locked after maximum attempts ownerId={} attempts={}",
                                                        owner.getId(),
                                                        attempts);

                                        throw new UnauthorizedException(
                                                        "Too many incorrect attempts. Please request a new code.");
                                }

                                otpRepository.save(loginOtp);

                                log.warn(
                                                "OTP verification failed ownerId={} attempts={}",
                                                owner.getId(),
                                                attempts);

                                throw new UnauthorizedException(
                                                "Invalid verification code.");
                        }

                        loginOtp.setUsed(true);

                        otpRepository.save(loginOtp);

                        String token = jwtService.generateToken(owner);

                        otpSuccessCounter.increment();
                        loginSuccessCounter.increment();

                        log.info(
                                        "OTP verification successful ownerId={}",
                                        owner.getId());

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

                } catch (UnauthorizedException ex) {

                        throw ex;

                } catch (Exception ex) {

                        loginFailureCounter.increment();

                        log.error(
                                        "OTP verification failed unexpectedly username={} errorType={}",
                                        username,
                                        ex.getClass().getSimpleName(),
                                        ex);

                        throw ex;

                } finally {

                        otpVerificationTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
        }

        private String generateOtp() {

                int otp = 100000 + secureRandom.nextInt(900000);

                return String.valueOf(otp);
        }

        private String maskEmail(String email) {

                if (email == null || email.isBlank()) {
                        return "unknown";
                }

                int atIndex = email.indexOf('@');

                if (atIndex <= 1) {
                        return "***";
                }

                return email.charAt(0)
                                + "***"
                                + email.substring(atIndex);
        }
}