package com.example.travelassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("itinerary_day_plan")
public class ItineraryDayPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itineraryId;

    private Integer dayNumber;

    private String routeSummary;

    private String routeDistance;

    private String routeDuration;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
