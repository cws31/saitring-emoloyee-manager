package com.sonuSaitring.sonuSaitringManagement.Hisab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;

@Repository
public interface HisabDetailRepository extends JpaRepository<HisabDetail, Long> {
}