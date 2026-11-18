package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionOption;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.service.EmployeeAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeAccessService employeeAccessService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           QuestionRepository questionRepository,
                           PasswordEncoder passwordEncoder,
                           EmployeeAccessService employeeAccessService) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeAccessService = employeeAccessService;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedSampleQuestions();
    }

    private void seedAdmin() {
        User admin;
        if (userRepository.existsByEmail(adminEmail)) {
            admin = userRepository.findByEmail(adminEmail).orElse(null);
        } else {
            admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setEmail(adminEmail);
            admin.setPhoneNumber("0000000000");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAddress("HQ");
            admin.setRole(Role.ADMIN);
            admin.setProfileCompleted(true);
            admin = userRepository.save(admin);
            System.out.println(">> Seeded default admin: " + adminEmail + " / " + adminPassword);
        }
        if (admin != null) {
            employeeAccessService.ensureSeedSuperAdmin(admin);
        }
    }

    private void seedSampleQuestions() {
        if (questionRepository.count() > 0) {
            return;
        }

        Question q1 = new Question();
        q1.setQuestionText("How would you describe your work-life balance?");
        q1.setType(QuestionType.SINGLE_CHOICE);
        q1.setCategory("Lifestyle");
        q1.setRequired(true);
        q1.setActive(true);
        addOptions(q1, List.of("Excellent", "Good", "Average", "Poor"));
        questionRepository.save(q1);

        Question q2 = new Question();
        q2.setQuestionText("Which activities are you interested in?");
        q2.setType(QuestionType.MULTI_CHOICE);
        q2.setCategory("Interest");
        q2.setRequired(false);
        q2.setActive(true);
        addOptions(q2, List.of("Sports", "Music", "Reading", "Travel", "Gaming", "Cooking"));
        questionRepository.save(q2);

        Question q3 = new Question();
        q3.setQuestionText("What are your career goals for the next 5 years?");
        q3.setType(QuestionType.TEXT);
        q3.setCategory("Occupation");
        q3.setRequired(false);
        q3.setActive(true);
        questionRepository.save(q3);
    }

    private void addOptions(Question question, List<String> options) {
        int order = 0;
        for (String text : options) {
            QuestionOption option = new QuestionOption();
            option.setQuestion(question);
            option.setOptionText(text);
            option.setDisplayOrder(order++);
            question.getOptions().add(option);
        }
    }
}