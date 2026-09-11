package com.sonuSaitring.sonuSaitringManagement.security;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

        private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

        private final RestClient restClient;

        @Value("${brevo.api-key}")
        private String brevoApiKey;

        @Value("${brevo.sender.email}")
        private String senderEmail;

        @Value("${brevo.sender.name}")
        private String senderName;

        public EmailServiceImpl(RestClient.Builder restClientBuilder) {
                this.restClient = restClientBuilder
                                .baseUrl("https://api.brevo.com/v3")
                                .build();
        }

        @Override
        public void sendOtpEmail(String toEmail, String otp) {

                logger.info("Preparing to send OTP email to recipient: {}", toEmail);

                try {

                        Map<String, Object> requestBody = Map.of(
                                        "sender", Map.of(
                                                        "name", senderName,
                                                        "email", senderEmail),
                                        "to", new Object[] {
                                                        Map.of("email", toEmail)
                                        },
                                        "subject",
                                        "Your Admin Login OTP - SonuSaitring Management",
                                        "textContent",
                                        "Your verification code for admin access is: "
                                                        + otp
                                                        + "\nThis code expires in 5 minutes.");

                        restClient.post()
                                        .uri("/smtp/email")
                                        .header("api-key", brevoApiKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(requestBody)
                                        .retrieve()
                                        .toBodilessEntity();

                        logger.info(
                                        "Successfully sent OTP email to recipient: {}",
                                        toEmail);

                } catch (Exception e) {

                        logger.error(
                                        "Failed to send OTP email to recipient: {}",
                                        toEmail,
                                        e);

                        throw new ExternalServiceException(
                                        "Failed to send OTP email through Brevo.",
                                        e);
                }
        }
}