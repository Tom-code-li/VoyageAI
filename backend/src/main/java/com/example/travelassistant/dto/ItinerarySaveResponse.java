package com.example.travelassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItinerarySaveResponse {

    /** 保存后的行程主键。前端可据此跳转详情页或继续后续操作。 */
    private Long itineraryId;
}
