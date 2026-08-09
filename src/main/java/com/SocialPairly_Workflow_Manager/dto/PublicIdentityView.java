package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;

import java.time.LocalDate;
import java.time.Period;

/**
 * Member-facing identity projection.
 * Never includes legal full name, exact DOB, or private contact details.
 */
public record PublicIdentityView(
    String displayName,
    Integer age,
    String gender,
    boolean genderVisible,
    String locationCity,
    String locationCountry
) {
    public static PublicIdentityView from(User user, UserProfile profile) {
        String preferred = user.getPreferredName();
        String displayName = (preferred != null && !preferred.isBlank())
                ? preferred.trim()
                : user.getFirstName();

        Integer age = null;
        String gender = null;
        boolean genderVisible = false;
        String locationCity = null;
        String locationCountry = null;

        if (profile != null) {
            if (profile.getDateOfBirth() != null) {
                age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            }
            String visibility = profile.getGenderShownToMatches();
            if (visibility == null || visibility.isBlank()) {
                visibility = "MATCHES";
            }
            genderVisible = "MATCHES".equalsIgnoreCase(visibility) || "PUBLIC".equalsIgnoreCase(visibility);
            if (genderVisible) {
                gender = profile.getGender();
            }
            locationCity = profile.getLocationCity();
            locationCountry = profile.getLocationCountry();
        }

        return new PublicIdentityView(displayName, age, gender, genderVisible, locationCity, locationCountry);
    }
}
