package com.example.travelassistant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiTravelPlanDTO {

    /** 行程标题。 */
    private String title;
    /** 目的地城市。 */
    private String destination;
    /** 开始日期，字符串形式，便于直接兼容大模型 JSON。 */
    private String startDate;
    /** 结束日期，字符串形式。 */
    private String endDate;
    /** 总天数。 */
    private Integer totalDays;
    /** 按天拆分后的行程安排。 */
    private List<DailyScheduleDTO> dailySchedules = new ArrayList<>();

    @Data
    public static class DailyScheduleDTO {
        /** 第几天。 */
        @JsonAlias("day_number")
        private Integer dayNumber;
        /** 单日路线自然语言摘要。 */
        private String routeSummary;
        /** 单日路线距离。 */
        private String routeDistance;
        /** 单日路线时长。 */
        private String routeDuration;
        /** 该天的景点列表，顺序即游玩顺序。 */
        private List<AttractionPlanItemDTO> attractions = new ArrayList<>();
    }

    @Data
    public static class AttractionPlanItemDTO {
        /** 本地景点主键。AI 新生成的景点在最初可能为空。 */
        private Long attractionId;
        /** 景点名称。 */
        private String name;
        /** 景点简介。 */
        private String description;
        /** 建议游玩时长。 */
        @JsonAlias("suggested_hours")
        private BigDecimal suggestedHours;
        /** 在当天中的顺序。 */
        @JsonAlias("sort_order")
        private Integer sortOrder;
        /** 经度。 */
        private BigDecimal longitude;
        /** 纬度。 */
        private BigDecimal latitude;
        /** 景点图片地址。 */
        private String imageUrl;
    }
}
