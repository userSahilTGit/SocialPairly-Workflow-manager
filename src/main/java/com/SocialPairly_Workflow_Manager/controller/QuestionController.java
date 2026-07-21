package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AnswerRequest;
import com.SocialPairly_Workflow_Manager.dto.QuestionDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.AnswerService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.QuestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CurrentUserService currentUserService;

    public QuestionController(QuestionService questionService,
                              AnswerService answerService,
                              CurrentUserService currentUserService) {
        this.questionService = questionService;
        this.answerService = answerService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<QuestionDto>> getQuestions() {
        User user = currentUserService.getCurrentUser();
        log.info("Fetching active questions for userId={}", user.getId());
        return ResponseEntity.ok(questionService.getActiveQuestionsForUser(user));
    }

    @PostMapping("/answers")
    public ResponseEntity<Map<String, String>> submitAnswers(@Valid @RequestBody List<AnswerRequest> answers) {
        User user = currentUserService.getCurrentUser();
        log.info("Submitting {} answers for userId={}", answers.size(), user.getId());
        answerService.saveAnswers(user, answers);
        return ResponseEntity.ok(Map.of("message", "Answers saved"));
    }
}