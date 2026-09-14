package com.sonuSaitring.sonuSaitringManagement.owner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Owner> findByUsername(String username);
}