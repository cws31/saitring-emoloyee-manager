package com.sonuSaitring.sonuSaitringManagement.owner.service;

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
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;
import com.sonuSaitring.sonuSaitringManagement.security.JwtService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OwnerServiceImpl implements OwnerService {

        private static final Logger log = LoggerFactory.getLogger(OwnerServiceImpl.class);

        private static final String OWNER_LOGO_PATH = "/api/owners/%d/logo";

        private final OwnerRepository ownerRepository;

        private final PasswordEncoder passwordEncoder;

        private final OwnerLogoService ownerLogoService;

        private final JwtService jwtService;

        private final OwnerOtpService ownerOtpService;

        /*
         * ============================================================
         * Metrics
         * ============================================================
         */

        private final Counter registrationSuccessCounter;

        private final Counter registrationFailureCounter;

        private final Counter loginSuccessCounter;

        private final Counter loginFailureCounter;

        private final Timer registrationTimer;

        private final Timer loginTimer;

        /*
         * ============================================================
         * Constructor
         * ============================================================
         */

        public OwnerServiceImpl(
                        OwnerRepository ownerRepository,
                        PasswordEncoder passwordEncoder,
                        OwnerLogoService ownerLogoService,
                        JwtService jwtService,
                        OwnerOtpService ownerOtpService,
                        MeterRegistry meterRegistry) {

                this.ownerRepository = ownerRepository;
                this.passwordEncoder = passwordEncoder;
                this.ownerLogoService = ownerLogoService;
                this.jwtService = jwtService;
                this.ownerOtpService = ownerOtpService;

                this.registrationSuccessCounter = Counter.builder(
                                "owner.registration.success")
                                .description(
                                                "Successful owner registrations")
                                .register(meterRegistry);

                this.registrationFailureCounter = Counter.builder(
                                "owner.registration.failure")
                                .description(
                                                "Failed owner registrations")
                                .register(meterRegistry);

                this.loginSuccessCounter = Counter.builder(
                                "owner.login.success")
                                .description(
                                                "Successful owner login flows")
                                .register(meterRegistry);

                this.loginFailureCounter = Counter.builder(
                                "owner.login.failure")
                                .description(
                                                "Failed owner login attempts")
                                .register(meterRegistry);

                this.registrationTimer = Timer.builder(
                                "owner.registration.duration")
                                .description(
                                                "Owner registration duration")
                                .register(meterRegistry);

                this.loginTimer = Timer.builder(
                                "owner.login.duration")
                                .description(
                                                "Owner login duration")
                                .register(meterRegistry);
        }

        /*
         * ============================================================
         * REGISTER
         * ============================================================
         */

        @Override
        @Transactional
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

                        /*
                         * ========================================================
                         * Create owner first
                         * ========================================================
                         *
                         * We need the generated owner ID because the logo is
                         * stored in owner_logos using owner_id.
                         */

                        Owner owner = new Owner();

                        owner.setOwnerName(
                                        request.getOwnerName()
                                                        .trim());

                        owner.setOrganizationName(
                                        request.getOrganizationName()
                                                        .trim());

                        owner.setEmail(email);

                        owner.setUsername(username);

                        owner.setPassword(
                                        passwordEncoder.encode(
                                                        request.getPassword()));

                        Owner savedOwner = ownerRepository.save(owner);

                        /*
                         * ========================================================
                         * Store logo in MySQL
                         * ========================================================
                         */

                        ownerLogoService.saveLogo(
                                        savedOwner.getId(),
                                        logo);

                        /*
                         * ========================================================
                         * Logo URL
                         * ========================================================
                         *
                         * This is no longer a physical filesystem URL.
                         *
                         * It points to our Spring Boot logo endpoint.
                         */

                        String logoUrl = buildLogoUrl(savedOwner.getId());

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
                                        logoUrl);

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

        /*
         * ============================================================
         * LOGIN
         * ============================================================
         */

        @Override
        @Transactional(readOnly = true)
        public OwnerLoginResponse login(
                        OwnerLoginRequest request) {

                long start = System.nanoTime();

                String username = request.getUsername()
                                .trim();

                log.info(
                                "Owner login started username={}",
                                username);

                try {

                        Owner owner = ownerRepository
                                        .findByUsername(username)
                                        .orElseThrow(() -> {

                                                loginFailureCounter
                                                                .increment();

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

                        /*
                         * Password is correct.
                         *
                         * OTP service handles:
                         *
                         * - OTP generation
                         * - hashing
                         * - Redis storage
                         * - expiration
                         * - resend cooldown
                         * - rate limiting
                         * - Brevo delivery
                         */

                        ownerOtpService.createAndSendOtp(
                                        owner.getId(),
                                        owner.getEmail(),
                                        owner.getOwnerName());

                        log.info(
                                        "Login OTP created and delivery requested ownerId={}",
                                        owner.getId());

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

        /*
         * ============================================================
         * VERIFY OTP
         * ============================================================
         */

        @Override
        @Transactional(readOnly = true)
        public OwnerLoginResponse verifyLoginOtp(
                        VerifyOtpRequest request) {

                String username = request.getUsername()
                                .trim();

                log.info(
                                "OTP verification started username={}",
                                username);

                try {

                        Owner owner = ownerRepository
                                        .findByUsername(username)
                                        .orElseThrow(() -> {

                                                log.warn(
                                                                "OTP verification rejected because username was not found username={}",
                                                                username);

                                                return new UnauthorizedException(
                                                                "Invalid verification request.");
                                        });

                        /*
                         * OwnerOtpService:
                         *
                         * - retrieves Redis OTP
                         * - checks expiration
                         * - checks attempts
                         * - verifies hash
                         * - consumes OTP
                         * - prevents concurrent verification
                         */

                        ownerOtpService.verifyOtp(
                                        owner.getId(),
                                        request.getOtp());

                        /*
                         * OTP is valid.
                         *
                         * Generate JWT only after successful OTP verification.
                         */

                        String token = jwtService.generateToken(owner);

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
                                        buildLogoUrl(owner.getId()));

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
                }
        }

        /*
         * ============================================================
         * LOGO URL
         * ============================================================
         */

        private String buildLogoUrl(Long ownerId) {

                return OWNER_LOGO_PATH.formatted(ownerId);
        }

        /*
         * ============================================================
         * EMAIL MASKING
         * ============================================================
         */

        private String maskEmail(
                        String email) {

                if (email == null
                                || email.isBlank()) {

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