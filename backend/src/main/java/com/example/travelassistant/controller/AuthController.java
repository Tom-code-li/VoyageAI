package com.example.travelassistant.controller;

import com.example.travelassistant.common.Result;
import com.example.travelassistant.dto.AuthRequest;
import com.example.travelassistant.dto.UserProfileResponse;
import com.example.travelassistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserProfileResponse> register(@Valid @RequestBody AuthRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<UserProfileResponse> login(@Valid @RequestBody AuthRequest request) {
        return Result.success(userService.login(request));
    }
}
