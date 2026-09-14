package com.sonuSaitring.sonuSaitringManagement.owner.service.Impl;

import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ConflictException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.TooManyRequestsException;
import com.sonuSaitring.sonuSaitringManagement.owner.otp.OtpChannel;
import com.sonuSaitring.sonuSaitringManagement.owner.otp.SmsOtpSender;
import com.sonuSaitring.sonuSaitringManagement.owner.service.BrevoEmailService;
import com.sonuSaitring.sonuSaitringManagement.owner.service.OwnerOtpService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class OwnerOtpServiceImpl implements OwnerOtpService {

        private static final Logger log = LoggerFactory.getLogger(OwnerOtpServiceImpl.class);

        private static final long OTP_EXPIRY_SECONDS = 5 * 60;

        private static final int MAX_ATTEMPTS = 5;

        private static final long RESEND_COOLDOWN_SECONDS = 60;

        private static final long RATE_LIMIT_WINDOW_SECONDS = 15 * 60;

        private static final int MAX_REQUESTS = 5;

        private static final long VERIFICATION_LOCK_SECONDS = 10;

        private static final String OTP_PREFIX = "owner:otp:";
        private static final String COOLDOWN_PREFIX = "owner:otp:cooldown:";
        private static final String RATE_PREFIX = "owner:otp:rate:";
        private static final String VERIFY_LOCK_PREFIX = "owner:otp:verify-lock:";

        private final StringRedisTemplate redisTemplate;
        private final PasswordEncoder passwordEncoder;
        private final BrevoEmailService brevoEmailService;
        private final SmsOtpSender smsOtpSender;

        private final SecureRandom secureRandom = new SecureRandom();

        private final Counter otpCreatedCounter;
        private final Counter deliveryFailureCounter;
        private final Counter verificationSuccessCounter;
        private final Counter verificationInvalidCounter;
        private final Counter verificationExpiredCounter;
        private final Counter verificationMaxAttemptsCounter;
        private final Counter rateLimitedCounter;
        private final Counter cooldownRejectedCounter;
        private final Counter verificationLockedCounter;

        private final Timer verificationDuration;

        public OwnerOtpServiceImpl(
                        StringRedisTemplate redisTemplate,
                        PasswordEncoder passwordEncoder,
                        BrevoEmailService brevoEmailService,
                        SmsOtpSender smsOtpSender,
                        MeterRegistry meterRegistry) {

                this.redisTemplate = redisTemplate;
                this.passwordEncoder = passwordEncoder;
                this.brevoEmailService = brevoEmailService;
                this.smsOtpSender = smsOtpSender;

                this.otpCreatedCounter = Counter.builder(
                                "owner.otp.created")
                                .register(meterRegistry);

                this.deliveryFailureCounter = Counter.builder(
                                "owner.otp.delivery.failure")
                                .register(meterRegistry);

                this.verificationSuccessCounter = Counter.builder(
                                "owner.otp.verification.success")
                                .register(meterRegistry);

                this.verificationInvalidCounter = Counter.builder(
                                "owner.otp.verification.invalid")
                                .register(meterRegistry);

                this.verificationExpiredCounter = Counter.builder(
                                "owner.otp.verification.expired")
                                .register(meterRegistry);

                this.verificationMaxAttemptsCounter = Counter.builder(
                                "owner.otp.verification.max_attempts")
                                .register(meterRegistry);

                this.rateLimitedCounter = Counter.builder(
                                "owner.otp.request.rate_limited")
                                .register(meterRegistry);

                this.cooldownRejectedCounter = Counter.builder(
                                "owner.otp.request.cooldown_rejected")
                                .register(meterRegistry);

                this.verificationLockedCounter = Counter.builder(
                                "owner.otp.verification.locked")
                                .register(meterRegistry);

                this.verificationDuration = Timer.builder(
                                "owner.otp.verification.duration")
                                .register(meterRegistry);
        }

        @Override
        public void createAndSendOtp(
                        Long ownerId,
                        String destination,
                        String ownerName,
                        OtpChannel channel) {

                String otpKey = OTP_PREFIX + ownerId;
                String cooldownKey = COOLDOWN_PREFIX + ownerId;
                String rateKey = RATE_PREFIX + ownerId;

                Long requestCount = redisTemplate.opsForValue().increment(rateKey);

                if (requestCount != null && requestCount == 1) {

                        redisTemplate.expire(
                                        rateKey,
                                        Duration.ofSeconds(
                                                        RATE_LIMIT_WINDOW_SECONDS));
                }

                if (requestCount != null && requestCount > MAX_REQUESTS) {

                        rateLimitedCounter.increment();

                        throw new TooManyRequestsException(
                                        "Too many verification code requests. Please try again later.");
                }

                Boolean cooldownExists = redisTemplate.hasKey(cooldownKey);

                if (Boolean.TRUE.equals(cooldownExists)) {

                        cooldownRejectedCounter.increment();

                        throw new TooManyRequestsException(
                                        "Please wait before requesting another verification code.");
                }

                String otp = generateOtp();

                String otpHash = passwordEncoder.encode(otp);

                Map<String, String> otpData = Map.of(
                                "otpHash", otpHash,
                                "attempts", "0",
                                "channel", channel.name());

                redisTemplate.opsForHash()
                                .putAll(otpKey, otpData);

                redisTemplate.expire(
                                otpKey,
                                Duration.ofSeconds(OTP_EXPIRY_SECONDS));

                redisTemplate.opsForValue()
                                .set(
                                                cooldownKey,
                                                "1",
                                                Duration.ofSeconds(
                                                                RESEND_COOLDOWN_SECONDS));

                try {

                        if (channel == OtpChannel.EMAIL) {

                                brevoEmailService.sendLoginOtp(
                                                destination,
                                                ownerName,
                                                otp);

                        } else if (channel == OtpChannel.SMS) {

                                smsOtpSender.sendLoginOtp(
                                                destination,
                                                ownerName,
                                                otp);

                        } else {

                                throw new BadRequestException(
                                                "Unsupported verification channel.");
                        }

                        otpCreatedCounter.increment();

                        log.info(
                                        "Owner login OTP created using {} channel for ownerId={}",
                                        channel,
                                        ownerId);

                } catch (ExternalServiceException ex) {

                        cleanupOtpAfterDeliveryFailure(
                                        otpKey,
                                        cooldownKey);

                        deliveryFailureCounter.increment();

                        log.error(
                                        "Owner OTP external service failure for ownerId={} channel={} exception={}",
                                        ownerId,
                                        channel,
                                        ex.getClass().getSimpleName());

                        throw ex;

                } catch (BadRequestException ex) {

                        cleanupOtpAfterDeliveryFailure(
                                        otpKey,
                                        cooldownKey);

                        throw ex;

                } catch (Exception ex) {

                        cleanupOtpAfterDeliveryFailure(
                                        otpKey,
                                        cooldownKey);

                        deliveryFailureCounter.increment();

                        log.error(
                                        "Unexpected OTP delivery failure for ownerId={} channel={} exception={}",
                                        ownerId,
                                        channel,
                                        ex.getClass().getSimpleName(),
                                        ex);

                        throw new ExternalServiceException(
                                        "Unable to send verification code. Please try again.",
                                        ex);
                }
        }

        @Override
        public void verifyOtp(
                        Long ownerId,
                        String otp) {

                Timer.Sample timer = Timer.start();

                String otpKey = OTP_PREFIX + ownerId;
                String lockKey = VERIFY_LOCK_PREFIX + ownerId;
                String lockToken = UUID.randomUUID().toString();

                try {

                        Boolean lockAcquired = redisTemplate.opsForValue()
                                        .setIfAbsent(
                                                        lockKey,
                                                        lockToken,
                                                        Duration.ofSeconds(
                                                                        VERIFICATION_LOCK_SECONDS));

                        if (!Boolean.TRUE.equals(lockAcquired)) {

                                verificationLockedCounter.increment();

                                throw new ConflictException(
                                                "Verification is already in progress.");
                        }

                        Map<Object, Object> data = redisTemplate.opsForHash()
                                        .entries(otpKey);

                        if (data == null || data.isEmpty()) {

                                verificationExpiredCounter.increment();

                                throw new BadRequestException(
                                                "Verification code has expired. Please request a new code.");
                        }

                        String otpHash = String.valueOf(data.get("otpHash"));

                        int attempts = Integer.parseInt(
                                        String.valueOf(
                                                        data.getOrDefault(
                                                                        "attempts",
                                                                        "0")));
                        if (attempts >= MAX_ATTEMPTS) {

                                verificationMaxAttemptsCounter.increment();

                                throw new BadRequestException(
                                                "Maximum verification attempts exceeded. Please request a new code.");
                        }

                        boolean valid = passwordEncoder.matches(
                                        otp,
                                        otpHash);

                        if (!valid) {

                                int newAttempts = attempts + 1;

                                redisTemplate.opsForHash()
                                                .put(
                                                                otpKey,
                                                                "attempts",
                                                                String.valueOf(newAttempts));

                                verificationInvalidCounter.increment();

                                if (newAttempts >= MAX_ATTEMPTS) {

                                        verificationMaxAttemptsCounter.increment();

                                        throw new BadRequestException(
                                                        "Invalid verification code. Please request a new code.");
                                }

                                throw new BadRequestException(
                                                "Invalid verification code.");
                        }

                        redisTemplate.delete(otpKey);

                        redisTemplate.delete(
                                        COOLDOWN_PREFIX + ownerId);

                        verificationSuccessCounter.increment();

                        log.info(
                                        "Owner OTP verified successfully for ownerId={}",
                                        ownerId);

                } finally {

                        releaseLock(
                                        lockKey,
                                        lockToken);

                        timer.stop(verificationDuration);
                }
        }

        @Override
        public void invalidateOtp(Long ownerId) {

                redisTemplate.delete(
                                OTP_PREFIX + ownerId);

                redisTemplate.delete(
                                COOLDOWN_PREFIX + ownerId);

                log.debug(
                                "Owner OTP invalidated for ownerId={}",
                                ownerId);
        }

        private String generateOtp() {

                int otp = secureRandom.nextInt(1_000_000);

                return String.format(
                                "%06d",
                                otp);
        }

        private void cleanupOtpAfterDeliveryFailure(
                        String otpKey,
                        String cooldownKey) {

                redisTemplate.delete(otpKey);
                redisTemplate.delete(cooldownKey);
        }

        private void releaseLock(
                        String lockKey,
                        String lockToken) {

                String currentToken = redisTemplate.opsForValue()
                                .get(lockKey);

                if (lockToken.equals(currentToken)) {

                        redisTemplate.delete(lockKey);
                }
        }
}
