package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.dto.QuestionDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionOption;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserAnswerRepository answerRepository;

    public QuestionService(QuestionRepository questionRepository, UserAnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    // ----- Admin -----

    @Transactional
    public Question create(QuestionRequest request) {
        Question q = new Question();
        applyRequest(q, request);
        return questionRepository.save(q);
    }

    @Transactional
    public Question update(Long id, QuestionRequest request) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        q.getOptions().clear();
        applyRequest(q, request);
        return questionRepository.save(q);
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Question not found: " + id);
        }
        questionRepository.deleteById(id);
    }

    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    private void applyRequest(Question q, QuestionRequest request) {
        q.setQuestionText(request.questionText());
        q.setType(request.type() == null ? QuestionType.TEXT : request.type());
        q.setCategory(request.category());
        q.setRequired(request.required());
        q.setActive(request.active());

        if (request.options() != null 
                && (q.getType() == QuestionType.SINGLE_CHOICE || q.getType() == QuestionType.MULTI_CHOICE)) {
            int order = 0;
            for (String text : request.options()) {
                if (text == null || text.isBlank()) {
                    continue;
                }
                QuestionOption option = new QuestionOption();
                option.setQuestion(q);
                option.setOptionText(text.trim());
                option.setDisplayOrder(order++);
                q.getOptions().add(option);
            }
        }
    }

    // ----- User -----

    public List<QuestionDto> getActiveQuestionsForUser(User user) {
        List<Question> questions = questionRepository.findByActiveTrueOrderByCreatedAtAsc();
        Map<Long, String> answersByQuestion = answerRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), UserAnswer::getAnswerValue, (a, b) -> b));

        List<QuestionDto> result = new ArrayList<>();
        for (Question q : questions) {
            result.add(QuestionDto.from(q, answersByQuestion.get(q.getId())));
        }
        return result;
    }
}