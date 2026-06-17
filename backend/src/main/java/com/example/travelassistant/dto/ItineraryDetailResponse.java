package com.example.travelassistant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItineraryDetailResponse {

    /** 行程主键。 */
    private Long id;
    /** 行程标题。 */
    private String title;
    /** 行程城市。 */
    private String city;
    /** 行程总天数。 */
    private Integer totalDays;
    /** 每天的详细安排。 */
    private List<DailyScheduleVO> dailySchedules = new ArrayList<>();

    @Data
    public static class DailyScheduleVO {
        private Integer dayNumber;
        /** 每天的路线摘要。 */
        private String routeSummary;
        private String routeDistance;
        private String routeDuration;
        /** 当天景点列表。 */
        private List<AttractionVO> attractions = new ArrayList<>();
    }

    @Data
    public static class AttractionVO {
        /** 行程明细主键，对应 itinerary_detail.id。 */
        private Long detailId;
        /** 景点主键，对应 attraction.id。 */
        private Long attractionId;
        private String name;
        private String description;
        private BigDecimal suggestedHours;
        private Integer sortOrder;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private String imageUrl;
    }
}
