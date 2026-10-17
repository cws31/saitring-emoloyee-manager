package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HisabRepository extends JpaRepository<Hisab, Long> {
        Optional<Hisab> findByOwnerIdAndYearAndMonth(Long ownerId, Integer year, Integer month);

        Optional<Hisab> findByIdAndOwnerId(Long id, Long ownerId);

        List<Hisab> findByOwnerIdOrderByYearAscMonthAsc(Long ownerId);

        Optional<Hisab> findTopByOwnerIdOrderByYearDescMonthDesc(Long ownerId);
}
