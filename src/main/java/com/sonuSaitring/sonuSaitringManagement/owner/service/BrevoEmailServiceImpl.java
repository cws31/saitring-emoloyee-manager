package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BrevoEmailServiceImpl implements BrevoEmailService {

    private final RestClient restClient;

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final Long templateId;

    public BrevoEmailServiceImpl(
            @Value("${brevo.api-key}") String apiKey,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName,
            @Value("${brevo.otp-template-id}") Long templateId) {

        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.templateId = templateId;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    @Override
    public void sendLoginOtp(
            String recipientEmail,
            String recipientName,
            String otp) {

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
    }
}