package com.sonuSaitring.sonuSaitringManagement.admin.service;

import com.sonuSaitring.sonuSaitringManagement.admin.entity.Admin;
import com.sonuSaitring.sonuSaitringManagement.admin.repository.AdminRepository;
import com.sonuSaitring.sonuSaitringManagement.security.EmailService;
import com.sonuSaitring.sonuSaitringManagement.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void initiateLogin(String username, String password) {
        logger.info("=== LOGIN ATTEMPT INITIATED for username: {} ===", username);

        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            logger.error("FAILED: Admin username '{}' not found in database!", username);
            throw new RuntimeException("Invalid username or password");
        }
        logger.info("SUCCESS: Admin found in database. Email registered: {}", admin.getEmail());

        boolean passwordMatches = passwordEncoder.matches(password, admin.getPassword());
        logger.info("Password match result: {}", passwordMatches);

        if (!passwordMatches) {
            logger.error("FAILED: Password does not match hash stored in database!");
            throw new RuntimeException("Invalid username or password");
        }

     
        String otp = String.format("%06d", new Random().nextInt(999999));
        admin.setOtp(otp);
        admin.setOtpGeneratedTime(LocalDateTime.now().plusMinutes(5));
        adminRepository.save(admin);
        logger.info("Generated OTP: {} for admin. Attempting to send email...", otp);

        try {
            emailService.sendOtpEmail(admin.getEmail(), otp);
            logger.info("SUCCESS: OTP email sent successfully to {}", admin.getEmail());
        } catch (Exception e) {
            logger.error("FAILED TO SEND EMAIL via SMTP: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email. Check mail configuration.");
        }
    }

    @Override
    public String verifyOtpAndGenerateToken(String username, String otp) {
        logger.info("=== OTP VERIFICATION ATTEMPT for username: {} with OTP: {} ===", username, otp);

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("FAILED: Admin not found during OTP verification: {}", username);
                    return new RuntimeException("Admin not found");
                });

        if (admin.getOtp() == null || !admin.getOtp().equals(otp)) {
            logger.error("FAILED: Provided OTP '{}' does not match stored OTP '{}'", otp, admin.getOtp());
            throw new RuntimeException("Invalid OTP");
        }

        if (admin.getOtpGeneratedTime().isBefore(LocalDateTime.now())) {
            logger.error("FAILED: OTP has expired. Generated time was: {}", admin.getOtpGeneratedTime());
            throw new RuntimeException("OTP has expired");
        }

  
        admin.setOtp(null);
        admin.setOtpGeneratedTime(null);
        adminRepository.save(admin);
        logger.info("SUCCESS: OTP verified successfully. Generating JWT token...");

        return jwtUtils.generateToken(admin.getUsername());
    }
}