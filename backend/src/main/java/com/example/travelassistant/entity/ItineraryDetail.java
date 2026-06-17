package com.example.travelassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("itinerary_detail")
public class ItineraryDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itineraryId;

    private Long attractionId;

    private Integer dayNumber;

    private Integer sortOrder;
}
