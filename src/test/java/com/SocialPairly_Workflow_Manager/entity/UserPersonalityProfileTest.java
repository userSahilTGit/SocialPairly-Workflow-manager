package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPersonalityProfileTest {

    @Test
    void friendDescriptorsNullSafeGetAndSet() {
        UserPersonalityProfile profile = new UserPersonalityProfile();
        assertTrue(profile.getFriendDescriptors().isEmpty());
        profile.setFriendDescriptors(null);
        assertTrue(profile.getFriendDescriptors().isEmpty());
        profile.setFriendDescriptors(List.of("kind"));
        assertEquals(List.of("kind"), profile.getFriendDescriptors());
    }

    @Test
    void personalityTraitsNullSafeGetAndSet() {
        UserPersonalityProfile profile = new UserPersonalityProfile();
        assertTrue(profile.getPersonalityTraits().isEmpty());
        profile.setPersonalityTraits(null);
        assertTrue(profile.getPersonalityTraits().isEmpty());
        profile.setPersonalityTraits(Set.of("curious"));
        assertEquals(Set.of("curious"), profile.getPersonalityTraits());
    }
}
