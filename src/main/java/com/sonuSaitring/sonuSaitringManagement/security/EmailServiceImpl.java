package com.sonuSaitring.sonuSaitringManagement.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Admin Login OTP - SonuSaitring Management");
        message.setText("Your verification code for admin access is: " + otp + "\nThis code expires in 5 minutes.");
        mailSender.send(message);
    }
}