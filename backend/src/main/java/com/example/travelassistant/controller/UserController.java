package com.example.travelassistant.controller;

import com.example.travelassistant.common.Result;
import com.example.travelassistant.dto.UserProfileResponse;
import com.example.travelassistant.dto.UserProfileUpdateRequest;
import com.example.travelassistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserProfileResponse> getProfile(@PathVariable Long id) {
        return Result.success(userService.getProfile(id));
    }

    @PutMapping
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return Result.success(userService.updateProfile(request));
    }
}
