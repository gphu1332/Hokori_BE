// com.hokori.web.service.JlptTestService.java
package com.hokori.web.service;

import com.hokori.web.dto.jlpt.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JlptTestService {

    private final JlptTestRepository testRepo;
    private final JlptQuestionRepository questionRepo;
    private final JlptOptionRepository optionRepo;
    private final JlptAnswerRepository answerRepo;
    private final UserRepository userRepo;

    @Transactional
    public JlptTest createTest(JlptEvent event, User moderator, JlptTestCreateRequest req) {
        JlptTest test = JlptTest.builder()
                .event(event)
                .createdBy(moderator)
                .level(req.getLevel())
                .durationMin(req.getDurationMin())
                .totalScore(req.getTotalScore())
                .result(req.getResultNote())
                .deletedFlag(false)
                .build();
        return testRepo.save(test);
    }

    @Transactional
    public JlptQuestionWithOptionsResponse createQuestion(Long testId,
                                                          User moderator,
                                                          JlptQuestionCreateRequest req) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        // (optional) kiểm tra moderator có quyền với test/event này không

        JlptQuestion q = JlptQuestion.builder()
                .test(test)
                .content(req.getContent())
                .questionType(req.getQuestionType())
                .explanation(req.getExplanation())
                .orderIndex(req.getOrderIndex())
                .audioPath(req.getAudioPath())
                .imagePath(req.getImagePath())
                .imageAltText(req.getImageAltText())
                .deletedFlag(false)
                .build();

        questionRepo.save(q);
        return JlptQuestionWithOptionsResponse.fromEntity(q, List.of());
    }

    @Transactional
    public JlptOptionResponse createOption(Long questionId,
                                           User moderator,
                                           JlptOptionCreateRequest req) {
        JlptQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        JlptOption o = JlptOption.builder()
                .question(q)
                .content(req.getContent())
                .isCorrect(Boolean.TRUE.equals(req.getCorrect()))
                .orderIndex(req.getOrderIndex())
                .imagePath(req.getImagePath())
                .imageAltText(req.getImageAltText())
                .build();

        optionRepo.save(o);
        return JlptOptionResponse.fromEntity(o);
    }

    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getQuestionsWithOptions(Long testId) {
        List<JlptQuestion> questions = questionRepo.findByTest_IdAndDeletedFlagFalseOrderByOrderIndexAsc(testId);
        return questions.stream().map(q -> {
            List<JlptOption> options = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId());
            List<JlptOptionResponse> optionDtos = options.stream()
                    .map(JlptOptionResponse::fromEntity)
                    .toList();
            return JlptQuestionWithOptionsResponse.fromEntity(q, optionDtos);
        }).toList();
    }

    @Transactional
    public void submitAnswer(Long testId, Long userId, JlptAnswerSubmitRequest req) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        JlptQuestion question = questionRepo.findById(req.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        if (!question.getTest().getId().equals(test.getId())) {
            throw new IllegalArgumentException("Question does not belong to this test");
        }

        JlptOption option = optionRepo.findById(req.getSelectedOptionId())
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));

        if (!option.getQuestion().getId().equals(question.getId())) {
            throw new IllegalArgumentException("Option does not belong to this question");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean correct = Boolean.TRUE.equals(option.getIsCorrect());

        // upsert: 1 user chỉ có 1 answer / question
        JlptAnswer answer = answerRepo
                .findByUser_IdAndTest_IdAndQuestion_Id(userId, testId, question.getId())
                .orElseGet(() -> JlptAnswer.builder()
                        .user(user)
                        .test(test)
                        .question(question)
                        .build());

        answer.setSelectedOption(option);
        answer.setIsCorrect(correct);
        answer.setAnsweredAt(java.time.Instant.now());

        answerRepo.save(answer);
    }

    @Transactional(readOnly = true)
    public JlptTestResultResponse getResultForUser(Long testId, Long userId) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        int totalQuestions = questionRepo.countByTest_IdAndDeletedFlagFalse(testId).intValue();
        int correctCount = answerRepo.countByUser_IdAndTest_IdAndIsCorrectTrue(userId, testId).intValue();

        double score = 0.0;
        if (totalQuestions > 0 && test.getTotalScore() != null) {
            score = (double) test.getTotalScore() * correctCount / totalQuestions;
        }

        return JlptTestResultResponse.builder()
                .testId(testId)
                .userId(userId)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .score(score)
                .build();
    }
}
