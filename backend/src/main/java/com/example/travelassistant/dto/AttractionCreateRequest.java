package com.example.travelassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttractionCreateRequest {

    @NotNull(message = "操作人ID不能为空")
    private Long operatorUserId;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "景点名称不能为空")
    private String name;

    private String description;

    private BigDecimal suggestedHours;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String imageUrl;
}
