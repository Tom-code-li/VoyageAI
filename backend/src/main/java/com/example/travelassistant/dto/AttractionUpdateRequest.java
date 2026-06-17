package com.example.travelassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttractionUpdateRequest {

    @NotNull(message = "景点ID不能为空")
    private Long id;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorUserId;

    private String city;

    private String name;

    private String description;

    private BigDecimal suggestedHours;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String imageUrl;
}
