package com.sonuSaitring.sonuSaitringManagement.security;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class JwtService {

        private static final Logger log = LoggerFactory.getLogger(JwtService.class);

        private final SecretKey signingKey;
        private final long expiration;

        private final Counter tokenGeneratedCounter;
        private final Counter tokenValidCounter;
        private final Counter tokenInvalidCounter;

        private final Timer tokenValidationTimer;

        public JwtService(
                        @Value("${jwt.secret}") String secret,
                        @Value("${jwt.expiration}") long expiration,
                        MeterRegistry meterRegistry) {

                this.signingKey = Keys.hmacShaKeyFor(
                                Decoders.BASE64.decode(secret));

                this.expiration = expiration;

                this.tokenGeneratedCounter = Counter.builder(
                                "security.jwt.generated")
                                .description("Number of JWT tokens generated")
                                .register(meterRegistry);

                this.tokenValidCounter = Counter.builder(
                                "security.jwt.valid")
                                .description("Number of valid JWT validations")
                                .register(meterRegistry);

                this.tokenInvalidCounter = Counter.builder(
                                "security.jwt.invalid")
                                .description("Number of invalid JWT validations")
                                .register(meterRegistry);

                this.tokenValidationTimer = Timer.builder(
                                "security.jwt.validation.duration")
                                .description("Time required to validate JWT tokens")
                                .register(meterRegistry);
        }

        public String generateToken(Owner owner) {

                Date now = new Date();

                Date expiryDate = new Date(
                                now.getTime() + expiration);

                String token = Jwts.builder()
                                .subject(owner.getId().toString())
                                .claim(
                                                "organizationName",
                                                owner.getOrganizationName())
                                .issuedAt(now)
                                .expiration(expiryDate)
                                .signWith(signingKey)
                                .compact();

                tokenGeneratedCounter.increment();

                log.debug(
                                "JWT generated ownerId={} expirationMs={}",
                                owner.getId(),
                                expiration);

                return token;
        }

        public String extractOwnerId(String token) {

                return extractAllClaims(token)
                                .getSubject();
        }

        public boolean isTokenValid(String token) {

                long start = System.nanoTime();

                try {

                        extractAllClaims(token);

                        tokenValidCounter.increment();

                        return true;

                } catch (Exception ex) {

                        tokenInvalidCounter.increment();

                        log.debug(
                                        "JWT validation failed errorType={}",
                                        ex.getClass().getSimpleName());

                        return false;

                } finally {

                        tokenValidationTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
        }

        private Claims extractAllClaims(String token) {

                return Jwts.parser()
                                .verifyWith(signingKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
        }
}