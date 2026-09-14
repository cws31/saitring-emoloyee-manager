package com.sonuSaitring.sonuSaitringManagement.owner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLogo;

public interface OwnerLogoRepository
        extends JpaRepository<OwnerLogo, Long> {

    Optional<OwnerLogo> findByOwnerId(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}