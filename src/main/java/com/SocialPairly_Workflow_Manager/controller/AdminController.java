package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.service.AdminService;
import com.SocialPairly_Workflow_Manager.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final QuestionService questionService;

    public AdminController(AdminService adminService, QuestionService questionService) {
        this.adminService = adminService;
        this.questionService = questionService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    // ----- Question management -----

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @PostMapping("/questions")
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.create(request));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Question deleted"));
    }
}