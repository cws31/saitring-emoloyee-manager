package com.sonuSaitring.sonuSaitringManagement.monthClosing.service;

import com.sonuSaitring.sonuSaitringManagement.monthClosing.dto.MonthClosingRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.entity.MonthClosing;

import java.util.List;

public interface MonthClosingService {
    MonthClosing previewMonthClosing(MonthClosingRequestDTO requestDTO);

    MonthClosing closeMonth(MonthClosingRequestDTO requestDTO);

    List<MonthClosing> getAllMonthClosings();

    MonthClosing getMonthClosingById(Long id);

    MonthClosing getMonthClosingByYearAndMonth(int year, int month);
}