package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefCountryTest {

    @Test
    void shouldExposeAllGettersAndSetters() {
        RefCountry country = new RefCountry();
        country.setCode("US");
        country.setName("United States");
        country.setPostalRegex("^\\d{5}$");
        country.setActive(true);

        assertEquals("US", country.getCode());
        assertEquals("United States", country.getName());
        assertEquals("^\\d{5}$", country.getPostalRegex());
        assertTrue(country.getActive());
    }

    @Test
    void activeShouldDefaultToTrue() {
        RefCountry country = new RefCountry();
        assertTrue(country.getActive());
    }
}
