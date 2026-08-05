package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("onCreate sets timestamps")
    void onCreate_setsTimestamps() {
        User user = new User();
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        ReflectionTestUtils.invokeMethod(user, "onCreate");
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("onUpdate sets updatedAt")
    void onUpdate_setsUpdatedAt() {
        User user = new User();
        user.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        ReflectionTestUtils.invokeMethod(user, "onUpdate");
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }

    @Test
    @DisplayName("role defaults to USER")
    void defaultValues() {
        User user = new User();
        assertEquals(Role.USER, user.getRole());
        assertFalse(user.isProfileCompleted());
        assertFalse(user.isVerified());
    }

    @Test
    @DisplayName("getters and setters work for all fields")
    void gettersSetters() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhoneNumber("1234567890");
        user.setPassword("secret");
        user.setAddress("123 Main St");
        user.setRole(Role.ADMIN);
        user.setProfileCompleted(true);
        user.setVerified(true);
        assertEquals(1L, user.getId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("1234567890", user.getPhoneNumber());
        assertEquals("secret", user.getPassword());
        assertEquals("123 Main St", user.getAddress());
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.isProfileCompleted());
        assertTrue(user.isVerified());
    }
}
