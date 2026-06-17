package com.example.travelassistant.service;

import com.example.travelassistant.dto.DashboardSummaryVO;

public interface DashboardService {

    DashboardSummaryVO getSummary(Long userId);

    long getTotalUserCount();
}
