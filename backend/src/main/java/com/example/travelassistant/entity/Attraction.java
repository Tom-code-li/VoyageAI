package com.example.travelassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("attraction")
public class Attraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String city;

    private String name;

    private String description;

    private String imageUrl;

    private Integer referenceCount;

    @TableField("play_time")
    private BigDecimal playTime;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
