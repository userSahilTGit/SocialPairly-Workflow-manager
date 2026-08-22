package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PublicIdentityViewTest {

    @Test
    void preferredNameWinsOverFirstName() {
        User user = user("Ada", "Ada Preferred");
        PublicIdentityView view = PublicIdentityView.from(user, null);
        assertEquals("Ada Preferred", view.displayName());
        assertNull(view.age());
        assertNull(view.gender());
        assertFalse(view.genderVisible());
        assertNull(view.locationCity());
        assertNull(view.locationCountry());
    }

    @Test
    void blankPreferredNameFallsBackToFirstName() {
        User user = user("Ada", "   ");
        assertEquals("Ada", PublicIdentityView.from(user, null).displayName());
    }

    @Test
    void nullPreferredNameFallsBackToFirstName() {
        User user = user("Ada", null);
        assertEquals("Ada", PublicIdentityView.from(user, null).displayName());
    }

    @Test
    void profileNullSkipsAgeGenderLocation() {
        User user = user("Ada", null);
        PublicIdentityView view = PublicIdentityView.from(user, null);
        assertNull(view.age());
        assertFalse(view.genderVisible());
    }

    @Test
    void ageComputedWhenDobPresent() {
        UserProfile profile = new UserProfile();
        profile.setDateOfBirth(LocalDate.now().minusYears(25));
        profile.setGenderShownToMatches("HIDDEN");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", null), profile);
        assertEquals(25, view.age());
        assertFalse(view.genderVisible());
        assertNull(view.gender());
    }

    @Test
    void nullDobLeavesAgeNull() {
        UserProfile profile = new UserProfile();
        profile.setGenderShownToMatches("MATCHES");
        profile.setGender("Woman");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", null), profile);
        assertNull(view.age());
        assertTrue(view.genderVisible());
        assertEquals("Woman", view.gender());
    }

    @Test
    void blankVisibilityDefaultsToMatches() {
        UserProfile profile = new UserProfile();
        profile.setGenderShownToMatches("  ");
        profile.setGender("Man");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", null), profile);
        assertTrue(view.genderVisible());
        assertEquals("Man", view.gender());
    }

    @Test
    void nullVisibilityDefaultsToMatches() {
        UserProfile profile = new UserProfile();
        profile.setGenderShownToMatches(null);
        profile.setGender("Non-binary");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", null), profile);
        assertTrue(view.genderVisible());
        assertEquals("Non-binary", view.gender());
    }

    @Test
    void publicVisibilityShowsGender() {
        UserProfile profile = new UserProfile();
        profile.setGenderShownToMatches("public");
        profile.setGender("Woman");
        profile.setLocationCity("Austin");
        profile.setLocationCountry("US");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", "Addie"), profile);
        assertTrue(view.genderVisible());
        assertEquals("Woman", view.gender());
        assertEquals("Austin", view.locationCity());
        assertEquals("US", view.locationCountry());
        assertEquals("Addie", view.displayName());
    }

    @Test
    void hiddenVisibilityHidesGender() {
        UserProfile profile = new UserProfile();
        profile.setGenderShownToMatches("HIDDEN");
        profile.setGender("Woman");
        PublicIdentityView view = PublicIdentityView.from(user("Ada", null), profile);
        assertFalse(view.genderVisible());
        assertNull(view.gender());
    }

    private static User user(String first, String preferred) {
        User user = new User();
        user.setFirstName(first);
        user.setPreferredName(preferred);
        return user;
    }
}
