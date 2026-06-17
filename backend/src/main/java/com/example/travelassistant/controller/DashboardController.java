package com.example.travelassistant.controller;

import com.example.travelassistant.common.Result;
import com.example.travelassistant.dto.DashboardSummaryVO;
import com.example.travelassistant.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary(@RequestParam(required = false) Long userId) {
        return Result.success(dashboardService.getSummary(userId));
    }

    @GetMapping("/userCount")
    public Result<Long> userCount() {
        return Result.success(dashboardService.getTotalUserCount());
    }
}
