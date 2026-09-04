package com.sonuSaitring.sonuSaitringManagement.Hisab.service;

import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;

public interface HisabService {
    List<Hisab> getAllMonthClosings();

    Hisab getMonthClosingById(Long id);

    Hisab getMonthClosingByYearAndMonth(int year, int month);

    HisabDetail markEmployeeHisabCompleted(Long detailId, boolean completed);
}