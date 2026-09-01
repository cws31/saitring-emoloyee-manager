package com.sonuSaitring.sonuSaitringManagement.monthClosing.controller;

import com.sonuSaitring.sonuSaitringManagement.monthClosing.dto.MonthClosingRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.entity.MonthClosing;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.service.MonthClosingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/month-closings")
public class MonthClosingController {

    @Autowired
    private MonthClosingService monthClosingService;

    @PostMapping("/preview")
    public ResponseEntity<MonthClosing> previewMonthClosing(@RequestBody MonthClosingRequestDTO requestDTO) {
        MonthClosing preview = monthClosingService.previewMonthClosing(requestDTO);
        return ResponseEntity.ok(preview);
    }

    @PostMapping
    public ResponseEntity<MonthClosing> closeMonth(@RequestBody MonthClosingRequestDTO requestDTO) {
        MonthClosing closing = monthClosingService.closeMonth(requestDTO);
        return ResponseEntity.ok(closing);
    }

    @GetMapping
    public ResponseEntity<List<MonthClosing>> getAllMonthClosings() {
        return ResponseEntity.ok(monthClosingService.getAllMonthClosings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthClosing> getMonthClosingById(@PathVariable Long id) {
        return ResponseEntity.ok(monthClosingService.getMonthClosingById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<MonthClosing> getMonthClosingByYearAndMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(monthClosingService.getMonthClosingByYearAndMonth(year, month));
    }
}