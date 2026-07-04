package com.SocialPairly_Workflow_Manager.dto;

public record AuthResponse(
    String token,
    UserDto user
) {}