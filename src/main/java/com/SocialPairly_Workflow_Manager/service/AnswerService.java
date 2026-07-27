package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AnswerRequest;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);
    private final UserAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public AnswerService(UserAnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public void saveAnswers(User user, List<AnswerRequest> answers) {
        log.info("Saving {} answers for userId={}", answers.size(), user.getId());
        for (AnswerRequest req : answers) {
            Question question = questionRepository.findById(req.questionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + req.questionId()));

            UserAnswer answer = answerRepository
                    .findByUserIdAndQuestionId(user.getId(), question.getId())
                    .orElseGet(() -> {
                        UserAnswer a = new UserAnswer();
                        a.setUser(user);
                        a.setQuestion(question);
                        return a;
                    });

            answer.setAnswerValue(req.answerValue());
            answerRepository.save(answer);
        }
    }
}