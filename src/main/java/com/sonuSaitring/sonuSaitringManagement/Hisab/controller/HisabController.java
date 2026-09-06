package com.sonuSaitring.sonuSaitringManagement.Hisab.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;

import java.util.List;

@RestController
@RequestMapping("/api/month-closings")
public class HisabController {

    @Autowired
    private HisabService monthClosingService;

    @GetMapping
    public ResponseEntity<List<Hisab>> getAllMonthClosings() {
        return ResponseEntity.ok(monthClosingService.getAllMonthClosings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hisab> getMonthClosingById(@PathVariable Long id) {
        return ResponseEntity.ok(monthClosingService.getMonthClosingById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Hisab> getMonthClosingByYearAndMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(monthClosingService.getMonthClosingByYearAndMonth(year, month));
    }

    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<Hisab> getMonthClosingByPathYearAndMonth(
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(monthClosingService.getMonthClosingByYearAndMonth(year, month));
    }

    @RequestMapping(value = "/detail/{detailId}/complete", method = { RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH })
    public ResponseEntity<HisabDetail> toggleHisabComplete(
            @PathVariable Long detailId,
            @RequestParam boolean completed) {
        HisabDetail updatedDetail = monthClosingService.markEmployeeHisabCompleted(detailId, completed);
        return ResponseEntity.ok(updatedDetail);
    }
}