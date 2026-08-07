package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;

public record UserDto(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String address,
    Role role,
    boolean profileCompleted,
    String subscriptionDetails
) {
    public static UserDto from(User u) {
        return new UserDto(
            u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
            u.getPhoneNumber(), u.getAddress(), u.getRole(), u.isProfileCompleted(),
            ""
        );
    }

    public static UserDto from(User u, boolean subscribed) {
        return new UserDto(
            u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
            u.getPhoneNumber(), u.getAddress(), u.getRole(), u.isProfileCompleted(),
            subscribed ? "Subscribed" : "Unsubscribed"
        );
    }
}