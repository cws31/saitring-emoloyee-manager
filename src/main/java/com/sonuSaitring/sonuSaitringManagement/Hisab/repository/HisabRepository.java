package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;

import java.util.Optional;

@Repository
public interface HisabRepository extends JpaRepository<Hisab, Long> {
    Optional<Hisab> findByYearAndMonth(int year, int month);

    boolean existsByYearAndMonth(int year, int month);
}