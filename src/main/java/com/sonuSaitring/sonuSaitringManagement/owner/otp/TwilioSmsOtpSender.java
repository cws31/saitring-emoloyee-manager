package com.sonuSaitring.sonuSaitringManagement.owner.otp;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsOtpSender implements SmsOtpSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsOtpSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsOtpSender(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromNumber) {

        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @PostConstruct
    public void initialize() {

        Twilio.init(accountSid, authToken);

        log.info("Twilio SMS provider initialized successfully.");
    }

    @Override
    public void sendLoginOtp(
            String recipientMobile,
            String recipientName,
            String otp) {

        try {

            String messageBody = "Hello " + recipientName
                    + ", your verification code is "
                    + otp
                    + ". It expires in 5 minutes.";

            Message message = Message.creator(
                    new PhoneNumber(recipientMobile),
                    new PhoneNumber(fromNumber),
                    messageBody).create();

            log.info(
                    "Login OTP SMS sent successfully to {}, messageSid={}",
                    maskMobile(recipientMobile),
                    message.getSid());

        } catch (Exception ex) {

            log.error(
                    "Failed to send login OTP SMS to {}",
                    maskMobile(recipientMobile),
                    ex);

            throw new IllegalStateException(
                    "Unable to send verification code.",
                    ex);
        }
    }

    private String maskMobile(String mobile) {

        if (mobile == null || mobile.length() < 6) {
            return "******";
        }

        return mobile.substring(0, 4)
                + "******"
                + mobile.substring(mobile.length() - 2);
    }
}