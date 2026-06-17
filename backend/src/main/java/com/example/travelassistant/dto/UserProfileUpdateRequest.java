package com.example.travelassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String username;

    private String password;
}
