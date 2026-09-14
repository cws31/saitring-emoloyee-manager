package com.sonuSaitring.sonuSaitringManagement.Hisab.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;

@RestController
@RequestMapping("/api/month-closings")
public class HisabController {

    private static final Logger logger = LoggerFactory.getLogger(HisabController.class);

    private final HisabService monthClosingService;

    public HisabController(HisabService monthClosingService) {
        this.monthClosingService = monthClosingService;
    }

    @GetMapping
    public ResponseEntity<List<Hisab>> getAllMonthClosings() {

        logger.info("Month closing list request received");

        List<Hisab> result = monthClosingService.getAllMonthClosings();

        logger.info(
                "Month closing list request completed: records={}",
                result.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hisab> getMonthClosingById(
            @PathVariable Long id) {

        logger.info(
                "Month closing lookup request received: id={}",
                id);

        Hisab result = monthClosingService.getMonthClosingById(id);

        logger.info(
                "Month closing lookup completed: id={}",
                id);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<Hisab> getMonthClosingByYearAndMonth(
            @RequestParam int year,
            @RequestParam int month) {

        logger.info(
                "Month closing search request received: year={}, month={}",
                year,
                month);

        Hisab result = monthClosingService.getMonthClosingByYearAndMonth(
                year,
                month);

        logger.info(
                "Month closing search completed: year={}, month={}",
                year,
                month);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<Hisab> getMonthClosingByPathYearAndMonth(
            @PathVariable int year,
            @PathVariable int month) {

        logger.info(
                "Month closing path request received: year={}, month={}",
                year,
                month);

        Hisab result = monthClosingService.getMonthClosingByYearAndMonth(
                year,
                month);

        logger.info(
                "Month closing path request completed: year={}, month={}",
                year,
                month);

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/detail/{detailId}/complete", method = {
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH
    })
    public ResponseEntity<HisabDetail> toggleHisabComplete(
            @PathVariable Long detailId,
            @RequestParam boolean completed) {

        logger.info(
                "Hisab completion update request received: detailId={}, completed={}",
                detailId,
                completed);

        HisabDetail updatedDetail = monthClosingService.markEmployeeHisabCompleted(
                detailId,
                completed);

        logger.info(
                "Hisab completion update completed: detailId={}, completed={}",
                detailId,
                completed);

        return ResponseEntity.ok(updatedDetail);
    }
}