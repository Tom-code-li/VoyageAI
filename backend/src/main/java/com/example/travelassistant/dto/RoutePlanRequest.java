package com.example.travelassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutePlanRequest {

    @NotNull(message = "草稿不能为空")
    private AiTravelPlanDTO draft;

    @NotNull(message = "天数不能为空")
    private Integer dayNumber;

    private String mode = "driving";
}
