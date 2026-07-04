package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileCompletionService {

    private final QuestionRepository questionRepository;
    private final UserAnswerRepository answerRepository;

    public ProfileCompletionService(QuestionRepository questionRepository, UserAnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    public Map<String, Object> calculate(User user, UserProfile profile) {
        int total = 10;
        int filled = 0;

        if (profile != null) {
            if (isFilled(profile.getProfilePhotoUrl())) filled++;
            if (isFilled(profile.getAboutMe())) filled++;
            if (isFilled(profile.getOccupation())) filled++;
            if (isFilled(profile.getLifestyle())) filled++;
            if (profile.getInterests() != null && !profile.getInterests().isEmpty()) filled++;
            if (isFilled(profile.getLocationCity()) || isFilled(profile.getLocationCountry())) filled++;
            if (profile.getEducations() != null && profile.getEducations().stream()
                    .anyMatch(e -> isFilled(e.getInstitution()))) filled++;
            if (profile.getDateOfBirth() != null) filled++;
            if (isFilled(profile.getGender())) filled++;
        }

        List<Question> activeQuestions = questionRepository.findByActiveTrueOrderByCreatedAtAsc();
        if (activeQuestions.isEmpty()) {
            filled++;
        } else {
            long answered = answerRepository.findByUserId(user.getId()).stream()
                    .filter(a -> a.getAnswerValue() != null && !a.getAnswerValue().isBlank())
                    .count();
            if (answered >= activeQuestions.size()) {
                filled++;
            } else if (answered > 0) {
                filled += (int) Math.round((double) answered / activeQuestions.size());
            }
        }

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