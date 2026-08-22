package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserIdentityBackgroundTest {

    @Test
    void shouldSetTimestampsOnCreateAndUpdate() {
        UserIdentityBackground background = new UserIdentityBackground();
        ReflectionTestUtils.invokeMethod(background, "onCreate");
        assertNotNull(background.getCreatedAt());
        assertNotNull(background.getUpdatedAt());

        LocalDateTime previous = background.getUpdatedAt();
        ReflectionTestUtils.invokeMethod(background, "onUpdate");
        assertTrue(!background.getUpdatedAt().isBefore(previous));
    }

    @Test
    void shouldExposeAllGettersAndSetters() {
        User user = new User();
        user.setId(1L);
        UserIdentityBackground background = new UserIdentityBackground();
        background.setId(10L);
        background.setUser(user);
        background.setLine1("123 Main");
        background.setLine2("Apt 4");
        background.setUnit("4B");
        background.setCity("Austin");
        background.setStateRegion("Texas");
        background.setPostalCode("78701");
        background.setCountryCode("US");
        background.setResidenceType("Rent");
        background.setMoveInMonth(6);
        background.setMoveInYear(2020);
        background.setWillingToRelocate("Yes");
        background.setEventTravelRadiusMiles(50);
        background.setCountryOfBirth("US");
        background.setPrimaryNationality("US");
        background.setCountryOfCitizenship("US");
        background.setCurrentCountryOfResidence("US");
        background.setResidencyCategory("Citizen");
        background.setInternationalRelocationPref("Open");
        background.setFutureSponsorshipRequired("No");
        background.setOpenToPartnerAbroad("Yes");
        background.setPreferredRelocateLocations("[\"CA\"]");
        background.setPreviousAddresses("[]");
        background.setAdditionalNationalities("[]");
        background.setLanguages("[\"en\"]");
        background.setPreferredFutureCountries("[\"US\"]");
        background.setCreatedAt(LocalDateTime.now());
        background.setUpdatedAt(LocalDateTime.now());

        assertEquals(10L, background.getId());
        assertSame(user, background.getUser());
        assertEquals("123 Main", background.getLine1());
        assertEquals("Apt 4", background.getLine2());
        assertEquals("4B", background.getUnit());
        assertEquals("Austin", background.getCity());
        assertEquals("Texas", background.getStateRegion());
        assertEquals("78701", background.getPostalCode());
        assertEquals("US", background.getCountryCode());
        assertEquals("Rent", background.getResidenceType());
        assertEquals(6, background.getMoveInMonth());
        assertEquals(2020, background.getMoveInYear());
        assertEquals("Yes", background.getWillingToRelocate());
        assertEquals(50, background.getEventTravelRadiusMiles());
        assertEquals("US", background.getCountryOfBirth());
        assertEquals("US", background.getPrimaryNationality());
        assertEquals("US", background.getCountryOfCitizenship());
        assertEquals("US", background.getCurrentCountryOfResidence());
        assertEquals("Citizen", background.getResidencyCategory());
        assertEquals("Open", background.getInternationalRelocationPref());
        assertEquals("No", background.getFutureSponsorshipRequired());
        assertEquals("Yes", background.getOpenToPartnerAbroad());
        assertEquals("[\"CA\"]", background.getPreferredRelocateLocations());
        assertEquals("[]", background.getPreviousAddresses());
        assertEquals("[]", background.getAdditionalNationalities());
        assertEquals("[\"en\"]", background.getLanguages());
        assertEquals("[\"US\"]", background.getPreferredFutureCountries());
    }
}
