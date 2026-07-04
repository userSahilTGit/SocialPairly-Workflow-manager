package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProfileCompletionService {

    public Map<String, Object> calculate(User user, UserProfile profile) {
        // ... upper business logic omitted/hidden in snippet ...

        int percentage = Math.min(100, Math.round((filled * 100f) / total));

        Map<String, Object> result = new HashMap<>();
        result.put("percentage", percentage);
        result.put("filledSections", filled);
        result.put("totalSections", total);
        result.put("profileCompleted", user.isProfileCompleted());
        return result;
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }
}