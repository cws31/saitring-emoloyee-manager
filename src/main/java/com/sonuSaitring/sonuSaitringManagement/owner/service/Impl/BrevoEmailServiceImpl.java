package com.sonuSaitring.sonuSaitringManagement.owner.service.Impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.sonuSaitring.sonuSaitringManagement.owner.service.BrevoEmailService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BrevoEmailServiceImpl implements BrevoEmailService {

        private static final Logger log = LoggerFactory.getLogger(BrevoEmailServiceImpl.class);

        private final RestClient restClient;

        private final String apiKey;
        private final String senderEmail;
        private final String senderName;
        private final Long templateId;

        private final Counter emailSentCounter;
        private final Counter emailFailedCounter;
        private final Timer emailTimer;

        public BrevoEmailServiceImpl(
                        @Value("${brevo.api-key}") String apiKey,
                        @Value("${brevo.sender-email}") String senderEmail,
                        @Value("${brevo.sender-name}") String senderName,
                        @Value("${brevo.otp-template-id}") Long templateId,
                        MeterRegistry meterRegistry) {

                this.apiKey = apiKey;
                this.senderEmail = senderEmail;
                this.senderName = senderName;
                this.templateId = templateId;

                this.restClient = RestClient.builder()
                                .baseUrl("https://api.brevo.com/v3")
                                .build();

                this.emailSentCounter = Counter.builder("brevo.email.sent")
                                .description("Number of emails successfully sent through Brevo")
                                .tag("type", "login_otp")
                                .register(meterRegistry);

                this.emailFailedCounter = Counter.builder("brevo.email.failed")
                                .description("Number of failed Brevo email requests")
                                .tag("type", "login_otp")
                                .register(meterRegistry);

                this.emailTimer = Timer.builder("brevo.email.duration")
                                .description("Time taken to send emails through Brevo")
                                .tag("type", "login_otp")
                                .register(meterRegistry);
        }

        @Override
        public void sendLoginOtp(
                        String recipientEmail,
                        String recipientName,
                        String otp) {

                long start = System.nanoTime();

                log.info(
                                "Sending login OTP email recipient={}",
                                maskEmail(recipientEmail));

                try {

                        Map<String, Object> requestBody = Map.of(
                                        "sender",
                                        Map.of(
                                                        "name", senderName,
                                                        "email", senderEmail),

                                        "to",
                                        new Object[] {
                                                        Map.of(
                                                                        "email", recipientEmail,
                                                                        "name", recipientName)
                                        },

                                        "templateId",
                                        templateId,

                                        "params",
                                        Map.of(
                                                        "name", recipientName,
                                                        "otp", otp,
                                                        "expiryMinutes", 5));

                        restClient.post()
                                        .uri("/smtp/email")
                                        .header("api-key", apiKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .body(requestBody)
                                        .retrieve()
                                        .toBodilessEntity();

                        emailSentCounter.increment();

                        log.info(
                                        "Login OTP email sent successfully recipient={}",
                                        maskEmail(recipientEmail));

                } catch (Exception ex) {

                        emailFailedCounter.increment();

                        log.error(
                                        "Failed to send login OTP email recipient={} errorType={}",
                                        maskEmail(recipientEmail),
                                        ex.getClass().getSimpleName(),
                                        ex);

                        throw ex;

                } finally {

                        emailTimer.record(
                                        System.nanoTime() - start,
                                        java.util.concurrent.TimeUnit.NANOSECONDS);
                }
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