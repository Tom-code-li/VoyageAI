package com.example.travelassistant.service;

import com.example.travelassistant.dto.AuthRequest;
import com.example.travelassistant.dto.UserProfileResponse;
import com.example.travelassistant.dto.UserProfileUpdateRequest;

public interface UserService {

    UserProfileResponse register(AuthRequest request);

    UserProfileResponse login(AuthRequest request);

    UserProfileResponse getProfile(Long id);

    UserProfileResponse updateProfile(UserProfileUpdateRequest request);
}
