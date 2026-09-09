package com.sonuSaitring.sonuSaitringManagement.common.config;

import com.sonuSaitring.sonuSaitringManagement.admin.entity.Admin;
import com.sonuSaitring.sonuSaitringManagement.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (adminRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }

        if (adminRepository.findByEmail(adminEmail).isPresent()) {
            throw new IllegalStateException(
                    "Admin email already exists: " + adminEmail);
        }

        Admin admin = new Admin();

        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));

        adminRepository.save(admin);

        System.out.println("Initial admin account created successfully.");
    }

}
