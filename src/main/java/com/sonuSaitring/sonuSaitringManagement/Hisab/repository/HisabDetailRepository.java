package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HisabDetailRepository extends JpaRepository<HisabDetail, Long> {

        @Query("""
                        SELECT d
                        FROM HisabDetail d
                        JOIN FETCH d.monthClosing h
                        WHERE d.id = :detailId
                          AND h.owner.id = :ownerId
                        """)
        Optional<HisabDetail> findByIdAndOwnerId(
                        @Param("detailId") Long detailId,
                        @Param("ownerId") Long ownerId);
}