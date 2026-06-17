package com.example.travelassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItineraryListItemVO {

    /** 行程主键。 */
    private Long id;
    private String title;
    private String city;
    /** 行程总天数。 */
    private Integer totalDays;
    /** 行程包含的景点数量。 */
    private Integer attractionCount;
    /** 列表封面图，通常取首个景点图片。 */
    private String coverImageUrl;
    /** 列表摘要，通常取第一天路线摘要。 */
    private String routeSummary;
    /** 创建时间，用于列表排序和展示。 */
    private LocalDateTime createTime;
}
