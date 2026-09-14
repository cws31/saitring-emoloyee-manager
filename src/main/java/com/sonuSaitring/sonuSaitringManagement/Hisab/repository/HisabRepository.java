package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HisabRepository extends JpaRepository<Hisab, Long> {

        Optional<Hisab> findByOwnerIdAndYearAndMonth(
                        Long ownerId,
                        int year,
                        int month);

        boolean existsByOwnerIdAndYearAndMonth(
                        Long ownerId,
                        int year,
                        int month);

        List<Hisab> findAllByOwnerId(Long ownerId);

        Optional<Hisab> findByIdAndOwnerId(
                        Long id,
                        Long ownerId);
}