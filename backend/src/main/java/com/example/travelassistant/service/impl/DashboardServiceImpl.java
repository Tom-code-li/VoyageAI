package com.example.travelassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.travelassistant.dto.DashboardSummaryVO;
import com.example.travelassistant.entity.Itinerary;
import com.example.travelassistant.mapper.AttractionMapper;
import com.example.travelassistant.mapper.ItineraryMapper;
import com.example.travelassistant.mapper.UserMapper;
import com.example.travelassistant.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 数据看板服务实现。
 * 主要提供系统级统计数字，供管理员后台展示。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

        private final AttractionMapper attractionMapper;
        private final ItineraryMapper itineraryMapper;
        private final UserMapper userMapper;

        @Override
        public DashboardSummaryVO getSummary(Long userId) {
                // attractionCount：公共景点库沉淀总量
                // userCount：系统总用户数
                // itineraryCount：可按 userId 维度统计，也可统计全站总行程数
                DashboardSummaryVO summary = new DashboardSummaryVO();
                summary.setAttractionCount(attractionMapper.selectCount(null));
                summary.setUserCount(getTotalUserCount());
                summary.setItineraryCount(itineraryMapper.selectCount(new LambdaQueryWrapper<Itinerary>()
                                .eq(userId != null, Itinerary::getUserId, userId)));
                return summary;
        }

        @Override
        public long getTotalUserCount() {
                // 某些场景 Mapper 可能返回 null，这里统一兜底为 0。
                Long total = userMapper.selectTotalUserCount();
                return total == null ? 0L : total;
        }

}
