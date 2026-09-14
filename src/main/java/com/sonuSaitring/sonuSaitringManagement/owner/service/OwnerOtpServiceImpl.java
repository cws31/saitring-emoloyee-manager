package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.UnauthorizedException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OwnerOtpServiceImpl implements OwnerOtpService {

    private static final Logger log = LoggerFactory.getLogger(OwnerOtpServiceImpl.class);

    /*
     * ============================================================
     * OTP configuration
     * ============================================================
     */

    private static final int OTP_EXPIRY_SECONDS = 5 * 60;

    private static final int MAX_OTP_ATTEMPTS = 5;

    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private static final int MAX_OTP_REQUESTS = 5;

    private static final int OTP_REQUEST_WINDOW_SECONDS = 15 * 60;

    /*
     * ============================================================
     * Redis key prefixes
     * ============================================================
     */

    private static final String OTP_KEY_PREFIX = "owner:otp:";

    private static final String OTP_COOLDOWN_KEY_PREFIX = "owner:otp:cooldown:";

    private static final String OTP_RATE_LIMIT_KEY_PREFIX = "owner:otp:rate:";

    private static final String OTP_VERIFY_LOCK_KEY_PREFIX = "owner:otp:verify-lock:";

    /*
     * ============================================================
     * Redis hash fields
     * ============================================================
     */

    private static final String FIELD_OTP_HASH = "otpHash";

    private static final String FIELD_ATTEMPTS = "attempts";

    /*
     * ============================================================
     * Redis scripts
     * ============================================================
     */

    /*
     * Atomically releases the verification lock only if the
     * current lock value belongs to this request.
     */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
            Long.class);

    /*
     * ============================================================
     * Dependencies
     * ============================================================
     */

    private final StringRedisTemplate redisTemplate;

    private final PasswordEncoder passwordEncoder;

    private final BrevoEmailService brevoEmailService;

    private final SecureRandom secureRandom = new SecureRandom();

    /*
     * ============================================================
     * Metrics
     * ============================================================
     */

    private final Counter otpCreatedCounter;

    private final Counter otpDeliveryFailureCounter;

    private final Counter otpSuccessCounter;

    private final Counter otpInvalidCounter;

    private final Counter otpExpiredCounter;

    private final Counter otpMaxAttemptsCounter;

    private final Counter otpRateLimitedCounter;

    private final Counter otpCooldownRejectedCounter;

    private final Counter otpVerificationLockedCounter;

    private final Timer otpVerificationTimer;

    /*
     * ============================================================
     * Constructor
     * ============================================================
     */

    public OwnerOtpServiceImpl(
            StringRedisTemplate redisTemplate,
            PasswordEncoder passwordEncoder,
            BrevoEmailService brevoEmailService,
            MeterRegistry meterRegistry) {

        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;

        this.otpCreatedCounter = Counter.builder("owner.otp.created")
                .description("Login OTPs created")
                .register(meterRegistry);

        this.otpDeliveryFailureCounter = Counter.builder("owner.otp.delivery.failure")
                .description("OTP email delivery failures")
                .register(meterRegistry);

        this.otpSuccessCounter = Counter.builder("owner.otp.verification.success")
                .description("Successful OTP verifications")
                .register(meterRegistry);

        this.otpInvalidCounter = Counter.builder("owner.otp.verification.invalid")
                .description("Invalid OTP verification attempts")
                .register(meterRegistry);

        this.otpExpiredCounter = Counter.builder("owner.otp.verification.expired")
                .description("Expired OTP verification attempts")
                .register(meterRegistry);

        this.otpMaxAttemptsCounter = Counter.builder("owner.otp.verification.max_attempts")
                .description(
                        "OTP verification attempts blocked because maximum attempts were reached")
                .register(meterRegistry);

        this.otpRateLimitedCounter = Counter.builder("owner.otp.request.rate_limited")
                .description("OTP requests rejected by rate limiting")
                .register(meterRegistry);

        this.otpCooldownRejectedCounter = Counter.builder("owner.otp.request.cooldown_rejected")
                .description("OTP requests rejected by resend cooldown")
                .register(meterRegistry);

        this.otpVerificationLockedCounter = Counter.builder("owner.otp.verification.locked")
                .description("OTP verification requests rejected because another verification is in progress")
                .register(meterRegistry);

        this.otpVerificationTimer = Timer.builder("owner.otp.verification.duration")
                .description("OTP verification duration")
                .register(meterRegistry);
    }

    /*
     * ============================================================
     * CREATE OTP
     * ============================================================
     */

    @Override
    public void createAndSendOtp(
            Long ownerId,
            String email,
            String ownerName) {

        /*
         * First protect the OTP endpoint from excessive requests.
         */
        enforceRequestRateLimit(ownerId);

        /*
         * Then enforce resend cooldown.
         */
        enforceResendCooldown(ownerId);

        String otp = generateOtp();

        String otpHash = passwordEncoder.encode(otp);

        String otpKey = getOtpKey(ownerId);

        /*
         * A new OTP replaces the previous OTP.
         *
         * Therefore only the newest OTP is valid.
         */
        redisTemplate.opsForHash().put(
                otpKey,
                FIELD_OTP_HASH,
                otpHash);

        redisTemplate.opsForHash().put(
                otpKey,
                FIELD_ATTEMPTS,
                "0");

        redisTemplate.expire(
                otpKey,
                OTP_EXPIRY_SECONDS,
                TimeUnit.SECONDS);

        /*
         * Cooldown is independent from OTP expiration.
         *
         * OTP = valid for 5 minutes
         * Resend = allowed only after 60 seconds
         */
        redisTemplate.opsForValue().set(
                getCooldownKey(ownerId),
                "1",
                RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS);

        otpCreatedCounter.increment();

        log.info(
                "Login OTP created ownerId={} expirySeconds={} cooldownSeconds={}",
                ownerId,
                OTP_EXPIRY_SECONDS,
                RESEND_COOLDOWN_SECONDS);

        try {

            /*
             * IMPORTANT:
             * Never log the OTP.
             */
            brevoEmailService.sendLoginOtp(
                    email,
                    ownerName,
                    otp);

            log.info(
                    "Login OTP email sent ownerId={} email={}",
                    ownerId,
                    maskEmail(email));

        } catch (Exception ex) {

            otpDeliveryFailureCounter.increment();

            /*
             * If email delivery failed, invalidate the OTP.
             *
             * The owner should request another OTP.
             */
            redisTemplate.delete(
                    otpKey);

            redisTemplate.delete(
                    getCooldownKey(ownerId));

            log.error(
                    "Login OTP email delivery failed ownerId={} errorType={}",
                    ownerId,
                    ex.getClass().getSimpleName(),
                    ex);

            throw ex;
        }
    }

    /*
     * ============================================================
     * VERIFY OTP
     * ============================================================
     */

    @Override
    public void verifyOtp(
            Long ownerId,
            String otp) {

        long start = System.nanoTime();

        String lockKey = getVerifyLockKey(ownerId);

        String lockToken = UUID.randomUUID().toString();

        try {

            /*
             * Prevent two simultaneous requests from consuming
             * the same OTP.
             *
             * SET NX EX is atomic in Redis.
             */
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    lockToken,
                    Duration.ofSeconds(10));

            if (!Boolean.TRUE.equals(lockAcquired)) {

                otpVerificationLockedCounter.increment();

                log.warn(
                        "OTP verification rejected because another verification is already in progress ownerId={}",
                        ownerId);

                throw new UnauthorizedException(
                        "Verification is already in progress. Please try again.");
            }

            String otpKey = getOtpKey(ownerId);

            Object hashValue = redisTemplate.opsForHash()
                    .get(
                            otpKey,
                            FIELD_OTP_HASH);

            Object attemptsValue = redisTemplate.opsForHash()
                    .get(
                            otpKey,
                            FIELD_ATTEMPTS);

            /*
             * Redis automatically removes an OTP after 5 minutes.
             */
            if (hashValue == null
                    || attemptsValue == null) {

                otpExpiredCounter.increment();

                log.warn(
                        "OTP verification failed because OTP is missing or expired ownerId={}",
                        ownerId);

                throw new UnauthorizedException(
                        "Verification code has expired. Please request a new code.");
            }

            int attempts;

            try {

                attempts = Integer.parseInt(
                        attemptsValue.toString());

            } catch (NumberFormatException ex) {

                /*
                 * Corrupted Redis state.
                 */
                redisTemplate.delete(otpKey);

                otpInvalidCounter.increment();

                log.error(
                        "Invalid OTP attempt counter in Redis ownerId={}",
                        ownerId,
                        ex);

                throw new UnauthorizedException(
                        "Invalid or expired verification code.");
            }

            if (attempts >= MAX_OTP_ATTEMPTS) {

                redisTemplate.delete(otpKey);

                otpMaxAttemptsCounter.increment();

                log.warn(
                        "OTP maximum attempts already reached ownerId={}",
                        ownerId);

                throw new UnauthorizedException(
                        "Too many incorrect attempts. Please request a new code.");
            }

            String otpHash = hashValue.toString();

            boolean matches = passwordEncoder.matches(
                    otp,
                    otpHash);

            if (!matches) {

                Long updatedAttempts = redisTemplate.opsForHash()
                        .increment(
                                otpKey,
                                FIELD_ATTEMPTS,
                                1);

                otpInvalidCounter.increment();

                if (updatedAttempts != null
                        && updatedAttempts >= MAX_OTP_ATTEMPTS) {

                    redisTemplate.delete(otpKey);

                    otpMaxAttemptsCounter.increment();

                    log.warn(
                            "OTP invalidated after maximum attempts ownerId={} attempts={}",
                            ownerId,
                            updatedAttempts);

                    throw new UnauthorizedException(
                            "Too many incorrect attempts. Please request a new code.");
                }

                log.warn(
                        "Invalid OTP ownerId={} attempts={}",
                        ownerId,
                        updatedAttempts);

                throw new UnauthorizedException(
                        "Invalid verification code.");
            }

            /*
             * OTP is single-use.
             *
             * Because we hold the Redis verification lock,
             * another verification request cannot concurrently
             * consume the same OTP.
             */
            redisTemplate.delete(
                    otpKey);

            /*
             * Also remove the cooldown.
             *
             * The user is now successfully authenticated.
             */
            redisTemplate.delete(
                    getCooldownKey(ownerId));

            otpSuccessCounter.increment();

            log.info(
                    "OTP verification successful ownerId={}",
                    ownerId);

        } finally {

            /*
             * Release the lock only if this request owns it.
             */
            redisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockToken);

            otpVerificationTimer.record(
                    System.nanoTime() - start,
                    TimeUnit.NANOSECONDS);
        }
    }

    /*
     * ============================================================
     * INVALIDATE OTP
     * ============================================================
     */

    @Override
    public void invalidateOtp(
            Long ownerId) {

        redisTemplate.delete(
                getOtpKey(ownerId));

        redisTemplate.delete(
                getCooldownKey(ownerId));

        log.debug(
                "OTP invalidated ownerId={}",
                ownerId);
    }

    /*
     * ============================================================
     * RATE LIMIT
     * ============================================================
     */

    private void enforceRequestRateLimit(
            Long ownerId) {

        String key = getRateLimitKey(ownerId);

        Long count = redisTemplate.opsForValue()
                .increment(key);

        if (count == null) {

            throw new UnauthorizedException(
                    "Unable to process OTP request.");
        }

        /*
         * First request creates the rate-limit window.
         */
        if (count == 1) {

            redisTemplate.expire(
                    key,
                    OTP_REQUEST_WINDOW_SECONDS,
                    TimeUnit.SECONDS);
        }

        if (count > MAX_OTP_REQUESTS) {

            otpRateLimitedCounter.increment();

            log.warn(
                    "OTP request rate limit exceeded ownerId={} count={}",
                    ownerId,
                    count);

            throw new UnauthorizedException(
                    "Too many verification code requests. Please try again later.");
        }
    }

    /*
     * ============================================================
     * RESEND COOLDOWN
     * ============================================================
     */

    private void enforceResendCooldown(
            Long ownerId) {

        String key = getCooldownKey(ownerId);

        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {

            otpCooldownRejectedCounter.increment();

            Long remaining = redisTemplate.getExpire(
                    key,
                    TimeUnit.SECONDS);

            log.warn(
                    "OTP resend cooldown active ownerId={} remainingSeconds={}",
                    ownerId,
                    remaining);

            throw new UnauthorizedException(
                    "Please wait before requesting another verification code.");
        }
    }

    /*
     * ============================================================
     * OTP GENERATION
     * ============================================================
     */

    private String generateOtp() {

        int otp = secureRandom.nextInt(1_000_000);

        return String.format(
                "%06d",
                otp);
    }

    /*
     * ============================================================
     * REDIS KEYS
     * ============================================================
     */

    private String getOtpKey(
            Long ownerId) {

        return OTP_KEY_PREFIX + ownerId;
    }

    private String getCooldownKey(
            Long ownerId) {

        return OTP_COOLDOWN_KEY_PREFIX + ownerId;
    }

    private String getRateLimitKey(
            Long ownerId) {

        return OTP_RATE_LIMIT_KEY_PREFIX + ownerId;
    }

    private String getVerifyLockKey(
            Long ownerId) {

        return OTP_VERIFY_LOCK_KEY_PREFIX + ownerId;
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