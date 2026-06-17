package com.example.travelassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("itinerary")
public class Itinerary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String city;

    private LocalDateTime createTime;
}
