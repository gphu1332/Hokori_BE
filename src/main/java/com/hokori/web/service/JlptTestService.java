// com.hokori.web.service.JlptTestService.java
package com.hokori.web.service;

import com.hokori.web.Enum.JLPTLevel;
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
    private final JlptUserTestSessionRepository sessionRepo;

    @Transactional
    public JlptTest createTest(JlptEvent event, User moderator, JlptTestCreateRequest req) {
        JlptTest test = JlptTest.builder()
                .event(event)
                .createdBy(moderator)
                .level(req.getLevel ())
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
        ensureTestNotExpired(testId, userId);

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

    @Transactional
    public JlptTestResponse updateTest(Long testId,
                                       User moderator,
                                       JlptTestUpdateRequest req) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        // TODO: nếu cần kiểm tra moderator có quyền với event/test này thì thêm ở đây

        test.setLevel(req.getLevel());
        test.setDurationMin(req.getDurationMin());
        test.setTotalScore(req.getTotalScore());
        test.setResult(req.getResultNote());

        // test đang được quản lý bởi JPA, chỉ cần return, không cần save()
        return JlptTestResponse.fromEntity(test);
    }

    @Transactional
    public void softDeleteTest(Long testId, User moderator) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        // TODO: kiểm tra quyền nếu muốn
        test.setDeletedFlag(true);
    }


    @Transactional
    public JlptQuestionWithOptionsResponse updateQuestion(Long testId,
                                                          Long questionId,
                                                          User moderator,
                                                          JlptQuestionUpdateRequest req) {
        JlptQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        if (!q.getTest().getId().equals(testId)) {
            throw new IllegalArgumentException("Question does not belong to this test");
        }

        // TODO: kiểm tra quyền nếu cần

        q.setContent(req.getContent());
        q.setQuestionType(req.getQuestionType());
        q.setExplanation(req.getExplanation());
        q.setOrderIndex(req.getOrderIndex());
        q.setAudioPath(req.getAudioPath());
        q.setImagePath(req.getImagePath());
        q.setImageAltText(req.getImageAltText());

        // load options để trả về giống getQuestionsWithOptions
        List<JlptOption> options = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId());
        List<JlptOptionResponse> optionDtos = options.stream()
                .map(JlptOptionResponse::fromEntity)
                .toList();

        return JlptQuestionWithOptionsResponse.fromEntity(q, optionDtos);
    }

    @Transactional
    public void softDeleteQuestion(Long testId,
                                   Long questionId,
                                   User moderator) {
        JlptQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));

        if (!q.getTest().getId().equals(testId)) {
            throw new IllegalArgumentException("Question does not belong to this test");
        }

        q.setDeletedFlag(true);
    }


    @Transactional
    public JlptOptionResponse updateOption(Long questionId,
                                           Long optionId,
                                           User moderator,
                                           JlptOptionUpdateRequest req) {
        JlptOption o = optionRepo.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));

        if (!o.getQuestion().getId().equals(questionId)) {
            throw new IllegalArgumentException("Option does not belong to this question");
        }

        // TODO: kiểm tra quyền nếu cần

        o.setContent(req.getContent());
        o.setIsCorrect(Boolean.TRUE.equals(req.getCorrect()));
        o.setOrderIndex(req.getOrderIndex());
        o.setImagePath(req.getImagePath());
        o.setImageAltText(req.getImageAltText());

        return JlptOptionResponse.fromEntity(o);
    }

    @Transactional
    public void deleteOption(Long questionId,
                             Long optionId,
                             User moderator) {
        JlptOption o = optionRepo.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));

        if (!o.getQuestion().getId().equals(questionId)) {
            throw new IllegalArgumentException("Option does not belong to this question");
        }

        optionRepo.delete(o);   // Option không có deletedFlag nên xoá cứng
    }


    @Transactional
    public JlptTestStartResponse startTest(Long testId, Long userId) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 1. Xoá answer cũ
        answerRepo.deleteByUser_IdAndTest_Id(userId, testId);

        java.time.Instant now = java.time.Instant.now();
        int durationMin = test.getDurationMin() != null ? test.getDurationMin() : 0;
        java.time.Instant expiresAt = now.plus(java.time.Duration.ofMinutes(durationMin));

        // 2. Tạo / update session
        JlptUserTestSession session = sessionRepo
                .findByTest_IdAndUser_Id(testId, userId)
                .orElse(null);

        if (session == null) {
            session = new JlptUserTestSession();
            session.setTest(test);
            session.setUser(user);
            session.setStartedAt(now);
            session.setExpiresAt(expiresAt);
            sessionRepo.save(session);

            // lần đầu user này thi test này -> tăng currentParticipants
            Integer cur = test.getCurrentParticipants();
            test.setCurrentParticipants(cur == null ? 1 : cur + 1);
        } else {
            // cho phép start lại nhưng không tăng người thi
            session.setStartedAt(now);
            session.setExpiresAt(expiresAt);
        }

        // 3. Lấy câu hỏi + options
        List<JlptQuestionWithOptionsResponse> questions = getQuestionsWithOptions(testId);

        // 4. Build response
        return JlptTestStartResponse.builder()
                .testId(testId)
                .userId(userId)
                // Nếu JlptTest.level là Enum:
                // .level(test.getLevel())
                // Nếu vẫn đang là String:
                .level(JLPTLevel.valueOf(test.getLevel()))
                .durationMin(test.getDurationMin())
                .totalScore(test.getTotalScore())
                .currentParticipants(test.getCurrentParticipants())
                .startedAt(now)
                .questions(questions)
                .build();
    }


    private void ensureTestNotExpired(Long testId, Long userId) {
        JlptUserTestSession session = sessionRepo
                .findByTest_IdAndUser_Id(testId, userId)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa start bài thi này"));

        if (java.time.Instant.now().isAfter(session.getExpiresAt())) {
            throw new IllegalStateException("Thời gian làm bài đã hết");
        }
    }


}
