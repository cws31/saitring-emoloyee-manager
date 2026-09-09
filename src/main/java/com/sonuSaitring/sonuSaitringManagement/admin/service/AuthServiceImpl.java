package com.sonuSaitring.sonuSaitringManagement.admin.service;

import com.sonuSaitring.sonuSaitringManagement.admin.entity.Admin;
import com.sonuSaitring.sonuSaitringManagement.admin.repository.AdminRepository;
import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.UnauthorizedException;
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

        logger.info(
                "Login attempt initiated for username: {}",
                username);

        Admin admin = adminRepository.findByUsername(username)
                .orElse(null);

        if (admin == null) {

            logger.warn(
                    "Login failed: admin username not found: {}",
                    username);

            throw new UnauthorizedException(
                    "Invalid username or password");
        }

        boolean passwordMatches = passwordEncoder.matches(
                password,
                admin.getPassword());

        if (!passwordMatches) {

            logger.warn(
                    "Login failed: invalid password for username: {}",
                    username);

            throw new UnauthorizedException(
                    "Invalid username or password");
        }

        String otp = String.format(
                "%06d",
                new Random().nextInt(1_000_000));

        admin.setOtp(otp);

        admin.setOtpGeneratedTime(
                LocalDateTime.now().plusMinutes(5));

        adminRepository.save(admin);

        logger.info(
                "OTP generated successfully for username: {}",
                username);

        try {

            emailService.sendOtpEmail(
                    admin.getEmail(),
                    otp);

            logger.info(
                    "OTP email sent successfully for username: {}",
                    username);

        } catch (Exception e) {

            logger.error(
                    "Failed to send OTP email for username: {}",
                    username,
                    e);

            throw new ExternalServiceException(
                    "Failed to send OTP email.",
                    e);
        }
    }

    @Override
    public String verifyOtpAndGenerateToken(
            String username,
            String otp) {

        logger.info(
                "OTP verification attempt for username: {}",
                username);

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> {

                    logger.warn(
                            "OTP verification failed: admin not found: {}",
                            username);

                    return new ResourceNotFoundException(
                            "Admin not found");
                });

        if (admin.getOtp() == null
                || !admin.getOtp().equals(otp)) {

            logger.warn(
                    "OTP verification failed for username: {}",
                    username);

            throw new BadRequestException(
                    "Invalid OTP");
        }

        if (admin.getOtpGeneratedTime() == null
                || admin.getOtpGeneratedTime()
                        .isBefore(LocalDateTime.now())) {

            logger.warn(
                    "OTP expired for username: {}",
                    username);

            throw new BadRequestException(
                    "OTP has expired");
        }

        admin.setOtp(null);
        admin.setOtpGeneratedTime(null);

        adminRepository.save(admin);

        logger.info(
                "OTP verified successfully for username: {}",
                username);

        return jwtUtils.generateToken(
                admin.getUsername());
    }
}