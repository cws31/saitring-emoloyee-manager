package com.sonuSaitring.sonuSaitringManagement.security;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {

        logger.info(
                "Preparing to send OTP email to recipient: {}",
                toEmail);

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(toEmail);

            message.setSubject(
                    "Your Admin Login OTP - SonuSaitring Management");

            message.setText(
                    "Your verification code for admin access is: "
                            + otp
                            + "\nThis code expires in 5 minutes.");

            mailSender.send(message);

            logger.info(
                    "Successfully sent OTP email to recipient: {}",
                    toEmail);

        } catch (Exception e) {

            logger.error(
                    "Failed to send OTP email to recipient: {}",
                    toEmail,
                    e);

            throw new ExternalServiceException(
                    "Failed to send OTP email. Please check SMTP configuration or network connectivity.",
                    e);
        }
    }
}