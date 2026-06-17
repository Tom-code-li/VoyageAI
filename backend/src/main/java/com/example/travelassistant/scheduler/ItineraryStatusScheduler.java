package com.example.travelassistant.scheduler;

import com.example.travelassistant.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItineraryStatusScheduler {

    private final ItineraryService itineraryService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshStatus() {
        log.info("开始执行行程状态刷新任务");
        itineraryService.refreshStatus();
        log.info("行程状态刷新完成");
    }
}
