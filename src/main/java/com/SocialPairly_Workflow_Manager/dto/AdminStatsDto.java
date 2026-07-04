package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;
import java.util.Map;

public record AdminStatsDto(
    long totalUsers,
    long totalAdmins,
    long completedProfiles,
    long incompleteProfiles,
    long newUsersLastDays,
    long totalQuestions,
    List<CountByLabel> registrationsByDay,
    List<CountByLabel> profileCompletion,
    List<CountByLabel> usersByRole,
    Map<String, List<CountByLabel>> answerDistribution
) {
    public record CountByLabel(String label, long count) {}
}