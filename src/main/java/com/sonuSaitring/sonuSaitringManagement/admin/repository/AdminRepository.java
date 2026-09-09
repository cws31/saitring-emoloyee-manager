package com.sonuSaitring.sonuSaitringManagement.admin.repository;

import com.sonuSaitring.sonuSaitringManagement.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByEmail(String email);

}
