package com.sonuSaitring.sonuSaitringManagement.owner.repository;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository
                extends JpaRepository<Owner, Long> {

        boolean existsByEmail(String email);

        boolean existsByMobileNumber(String mobileNumber);

        boolean existsByEmailAndIdNot(
                        String email,
                        Long id);

        boolean existsByMobileNumberAndIdNot(
                        String mobileNumber,
                        Long id);

        Optional<Owner> findByEmail(String email);

        Optional<Owner> findByMobileNumber(String mobileNumber);

        Optional<Owner> findByEmailOrMobileNumber(
                        String email,
                        String mobileNumber);
}