package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminPaymentDto;
import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto.CountByLabel;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository answerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public AdminService(UserRepository userRepository,
                        QuestionRepository questionRepository,
                        UserAnswerRepository answerRepository,
                        SubscriptionRepository subscriptionRepository,
                        PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<UserDto> listUsers() {
        log.info("Listing all users");
        Set<Long> subscribedUserIds = new HashSet<>(
                subscriptionRepository.findActiveSubscribedUserIds(LocalDateTime.now())
        );
        return userRepository.findAll().stream()
                .map(user -> UserDto.from(user, subscribedUserIds.contains(user.getId())))
                .collect(Collectors.toList());
    }

    public List<AdminPaymentDto> listPayments() {
        log.info("Listing all payments for admin");
        return paymentRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(AdminPaymentDto::from)
                .collect(Collectors.toList());
    }

    public AdminStatsDto getStats() {
        log.info("Generating admin dashboard stats");
        List<User> users = userRepository.findAll();

        long totalUsers = users.size();
        long totalAdmins = users.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long completed = users.stream().filter(User::isProfileCompleted).count();
        long incomplete = totalUsers - completed;

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long newLast7 = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        long totalQuestions = questionRepository.count();

        // Registrations grouped per day for the last 7 days
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        Map<LocalDate, Long> regByDay = users.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<CountByLabel> registrationsByDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            registrationsByDay.add(new CountByLabel(day.format(fmt), regByDay.getOrDefault(day, 0L)));
        }

        List<CountByLabel> profileCompletion = List.of(
                new CountByLabel("Completed", completed),
                new CountByLabel("Incomplete", incomplete)
        );

        List<CountByLabel> usersByRole = List.of(
                new CountByLabel("User", totalUsers - totalAdmins),
                new CountByLabel("Admin", totalAdmins)
        );

        // Answer distribution for choice-based questions
        Map<String, List<CountByLabel>> answerDistribution = new LinkedHashMap<>();
        for (Question q : questionRepository.findAll()) {
            if (q.getType() == QuestionType.SINGLE_CHOICE || q.getType() == QuestionType.MULTI_CHOICE) {
                Map<String, Long> counts = new LinkedHashMap<>();
                q.getOptions().forEach(o -> counts.put(o.getOptionText(), 0L));

                answerRepository.findAll().stream()
                        .filter(a -> a.getQuestion().getId().equals(q.getId()))
                        .filter(a -> a.getAnswerValue() != null && !a.getAnswerValue().isBlank())
                        .forEach(a -> {
                            for (String value : a.getAnswerValue().split(",")) {
                                String key = value.trim();
                                if (counts.containsKey(key)) {
                                    counts.merge(key, 1L, Long::sum);
                                }
                            }
                        });

                List<CountByLabel> dist = counts.entrySet().stream()
                        .map(e -> new CountByLabel(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

                answerDistribution.put(q.getQuestionText(), dist);
            }
        }

        return new AdminStatsDto(
                totalUsers, totalAdmins, completed, incomplete, newLast7, totalQuestions,
                registrationsByDay, profileCompletion, usersByRole, answerDistribution
        );
    }
}