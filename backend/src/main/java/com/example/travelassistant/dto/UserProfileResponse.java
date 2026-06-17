package com.example.travelassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileResponse {

    private Long id;
    private String username;
    private String role;
    private Long itineraryCount;
    private LocalDateTime createTime;
}
