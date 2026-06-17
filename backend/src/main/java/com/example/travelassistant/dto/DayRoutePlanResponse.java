package com.example.travelassistant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DayRoutePlanResponse {

    /** 当前规划的是第几天。 */
    private Integer dayNumber;
    /** 路线模式，目前主要是 driving。 */
    private String mode;
    /** 所属城市。 */
    private String destination;
    /** 是否回退为简化结果。true 表示高德完整路线不可用。 */
    private boolean fallback;
    /** 给前端的提示信息，例如回退原因。 */
    private String message;
    /** 单日路线摘要。 */
    private String summary;
    /** 景点点位列表。 */
    private List<RoutePointDTO> points = new ArrayList<>();
    /** 路线集合。paths 和 routes 都保留，是为了兼容不同前端字段读取习惯。 */
    private List<RoutePathDTO> paths = new ArrayList<>();
    private List<RoutePathDTO> routes = new ArrayList<>();

    @Data
    public static class RoutePointDTO {
        private String name;
        private Integer sortOrder;
        private BigDecimal longitude;
        private BigDecimal latitude;
    }

    @Data
    public static class RoutePathDTO {
        /** 总距离，通常是米。 */
        private String distance;
        /** 总时长，通常是秒。 */
        private String duration;
        /** 高德返回的路线策略说明。 */
        private String strategy;
        /** 路线折线点。 */
        private List<RouteLinePointDTO> polyline = new ArrayList<>();
        /** 导航步骤。 */
        private List<RouteStepDTO> steps = new ArrayList<>();
    }

    @Data
    public static class RouteStepDTO {
        /** 步骤说明，例如“沿某路向北行驶”。 */
        private String instruction;
        private String road;
        private String orientation;
        private String distance;
        private String duration;
        private List<RouteLinePointDTO> polyline = new ArrayList<>();
    }

    @Data
    public static class RouteLinePointDTO {
        private BigDecimal longitude;
        private BigDecimal latitude;
    }
}
