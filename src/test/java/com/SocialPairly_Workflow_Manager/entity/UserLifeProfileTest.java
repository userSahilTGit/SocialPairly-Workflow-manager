package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserLifeProfileTest {

    @Test
    void onCreateInitializesNullShowEmployerPublicly() {
        UserLifeProfile life = new UserLifeProfile();
        life.setShowEmployerPublicly(null);
        life.onCreate();
        assertFalse(life.getShowEmployerPublicly());
        assertNotNull(life.getCreatedAt());
        assertNotNull(life.getUpdatedAt());
    }

    @Test
    void onCreatePreservesExistingShowEmployerPublicly() {
        UserLifeProfile life = new UserLifeProfile();
        life.setShowEmployerPublicly(true);
        life.onCreate();
        assertTrue(life.getShowEmployerPublicly());
    }
}
