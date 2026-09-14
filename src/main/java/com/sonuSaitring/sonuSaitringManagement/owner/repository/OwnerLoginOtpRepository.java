package com.sonuSaitring.sonuSaitringManagement.owner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLoginOtp;

public interface OwnerLoginOtpRepository
        extends JpaRepository<OwnerLoginOtp, Long> {

    Optional<OwnerLoginOtp> findTopByOwnerIdAndUsedFalseOrderByCreatedAtDesc(
            Long ownerId);
}