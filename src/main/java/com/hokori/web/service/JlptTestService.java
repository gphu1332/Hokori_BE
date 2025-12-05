// com.hokori.web.service.JlptTestService.java
package com.hokori.web.service;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.Enum.JlptQuestionType;
import com.hokori.web.dto.jlpt.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
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
    private final JlptTestAttemptRepository attemptRepo;
    private final JlptTestAttemptAnswerRepository attemptAnswerRepo;
    private final LearnerProgressService learnerProgressService;
    private final FileStorageService fileStorageService;

    private static final int DEFAULT_TOTAL_SCORE = 180;

    @Transactional
    public JlptTest createTest(JlptEvent event, User moderator, JlptTestCreateRequest req) {
        JlptTest test = JlptTest.builder()
                .event(event)
                .createdBy(moderator)
                .level(req.getLevel())
                .durationMin(req.getDurationMin())
                .totalScore(DEFAULT_TOTAL_SCORE)
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

        // Validate audioPath exists in file_storage if provided
        if (req.getAudioPath() != null && !req.getAudioPath().isEmpty()) {
            // Remove /files/ prefix if present (filePath should be relative path)
            String filePath = req.getAudioPath().startsWith("/files/")
                    ? req.getAudioPath().substring("/files/".length())
                    : req.getAudioPath();

            if (fileStorageService.getFile(filePath) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audio file not found in storage: " + filePath + ". Please upload the file first.");
            }
        }

        // Validate imagePath exists in file_storage if provided
        if (req.getImagePath() != null && !req.getImagePath().isEmpty()) {
            String filePath = req.getImagePath().startsWith("/files/")
                    ? req.getImagePath().substring("/files/".length())
                    : req.getImagePath();

            if (fileStorageService.getFile(filePath) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image file not found in storage: " + filePath + ". Please upload the file first.");
            }
        }

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
        List<JlptQuestion> questions =
                questionRepo.findByTest_IdAndDeletedFlagFalseOrderByOrderIndexAsc(testId);
        return mapQuestionsWithOptions(questions);
    }
    
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getQuestionsWithOptions(Long testId, Long userId) {
        List<JlptQuestion> questions =
                questionRepo.findByTest_IdAndDeletedFlagFalseOrderByOrderIndexAsc(testId);
        return mapQuestionsWithOptions(questions, userId);
    }

    // === LISTENING ===
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getListeningQuestions(Long testId) {
        return getListeningQuestions(testId, null);
    }
    
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getListeningQuestions(Long testId, Long userId) {
        List<JlptQuestion> questions =
                questionRepo.findByTest_IdAndQuestionTypeAndDeletedFlagFalseOrderByOrderIndexAsc(
                        testId,
                        JlptQuestionType.LISTENING
                );
        return mapQuestionsWithOptions(questions, userId);
    }

    // === READING ===
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getReadingQuestions(Long testId) {
        return getReadingQuestions(testId, null);
    }
    
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getReadingQuestions(Long testId, Long userId) {
        List<JlptQuestion> questions =
                questionRepo.findByTest_IdAndQuestionTypeAndDeletedFlagFalseOrderByOrderIndexAsc(
                        testId,
                        JlptQuestionType.READING
                );
        return mapQuestionsWithOptions(questions, userId);
    }

    // === GRAMMAR + VOCAB ===
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getGrammarVocabQuestions(Long testId) {
        return getGrammarVocabQuestions(testId, null);
    }
    
    @Transactional(readOnly = true)
    public List<JlptQuestionWithOptionsResponse> getGrammarVocabQuestions(Long testId, Long userId) {
        java.util.List<JlptQuestionType> types = java.util.List.of(
                JlptQuestionType.GRAMMAR,
                JlptQuestionType.VOCAB
        );
        List<JlptQuestion> questions =
                questionRepo.findByTest_IdAndQuestionTypeInAndDeletedFlagFalseOrderByOrderIndexAsc(
                        testId,
                        types
                );
        return mapQuestionsWithOptions(questions, userId);
    }


    // helper dùng lại cho các hàm bên dưới
    private List<JlptQuestionWithOptionsResponse> mapQuestionsWithOptions(List<JlptQuestion> questions) {
        return mapQuestionsWithOptions(questions, null);
    }
    
    // helper với userId để load saved answers
    private List<JlptQuestionWithOptionsResponse> mapQuestionsWithOptions(List<JlptQuestion> questions, Long userId) {
        // Load all saved answers for this user and test (if userId provided)
        java.util.Map<Long, Long> savedAnswers = new java.util.HashMap<>();
        if (userId != null && !questions.isEmpty()) {
            Long testId = questions.get(0).getTest().getId();
            List<JlptAnswer> answers = answerRepo.findByUser_IdAndTest_Id(userId, testId);
            savedAnswers = answers.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            a -> a.getQuestion().getId(),
                            a -> a.getSelectedOption().getId()
                    ));
        }
        
        final java.util.Map<Long, Long> finalSavedAnswers = savedAnswers;
        return questions.stream().map(q -> {
            List<JlptOption> options = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId());
            List<JlptOptionResponse> optionDtos = options.stream()
                    .map(JlptOptionResponse::fromEntity)
                    .toList();
            Long selectedOptionId = finalSavedAnswers.get(q.getId());
            return JlptQuestionWithOptionsResponse.fromEntity(q, optionDtos, selectedOptionId);
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
        Instant now = java.time.Instant.now();

        // Use native query with ON CONFLICT UPDATE to handle race conditions at database level
        // This is more reliable than try-catch approach
        try {
            answerRepo.upsertAnswer(
                    userId,
                    testId,
                    question.getId(),
                    option.getId(),
                    correct,
                    now,
                    now
            );
        } catch (Exception e) {
            // Fallback: if native query fails, try traditional approach
            // This should rarely happen, but provides safety net
            JlptAnswer answer = answerRepo
                    .findByUser_IdAndTest_IdAndQuestion_Id(userId, testId, question.getId())
                    .orElse(null);

            if (answer == null) {
                // Create new answer if not exists
                answer = JlptAnswer.builder()
                        .user(user)
                        .test(test)
                        .question(question)
                        .selectedOption(option)
                        .isCorrect(correct)
                        .answeredAt(now)
                        .build();
            } else {
                // Update existing answer
                answer.setSelectedOption(option);
                answer.setIsCorrect(correct);
                answer.setAnsweredAt(now);
            }

            answerRepo.save(answer);
        }
    }

    @Transactional(readOnly = true)
    public JlptTestResultResponse getResultForUser(Long testId, Long userId) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        // Kiểm tra xem có session đang làm bài không (chưa submit)
        java.util.Optional<JlptUserTestSession> sessionOpt = sessionRepo.findByTest_IdAndUser_Id(testId, userId);
        
        if (sessionOpt.isPresent()) {
            // Có session đang làm bài → trả về kết quả từ session hiện tại (chưa nộp)
            return getResultFromCurrentSession(testId, userId, test);
        } else {
            // Không có session → lấy attempt mới nhất đã submit
            List<JlptTestAttempt> attempts = attemptRepo.findByUser_IdAndTest_IdOrderBySubmittedAtDesc(userId, testId);
            if (attempts.isEmpty()) {
                // Chưa làm bài lần nào → trả về kết quả rỗng
                return getEmptyResult(testId, userId, test);
            } else {
                // Trả về attempt mới nhất
                return convertAttemptToResultResponse(attempts.get(0), test);
            }
        }
    }

    /**
     * Tính kết quả từ session hiện tại (chưa nộp bài).
     */
    private JlptTestResultResponse getResultFromCurrentSession(Long testId, Long userId, JlptTest test) {
        // ====== TỔNG ĐIỂM ======
        int totalQuestions = questionRepo.countByTest_IdAndDeletedFlagFalse(testId).intValue();
        int correctCount = answerRepo.countByUser_IdAndTest_IdAndIsCorrectTrue(userId, testId).intValue();

        double score = 0.0;
        int totalMax = test.getTotalScore() != null ? test.getTotalScore() : 180;
        if (totalQuestions > 0 && totalMax > 0) {
            score = (double) totalMax * correctCount / totalQuestions;
        }

        // ====== ĐIỂM TỪNG PHẦN ======
        // 1. Grammar + Vocab (gộp chung)
        java.util.List<JlptQuestionType> grammarVocabTypes = java.util.List.of(
                JlptQuestionType.GRAMMAR,
                JlptQuestionType.VOCAB
        );
        int grammarVocabTotal = questionRepo.countByTest_IdAndQuestionTypeInAndDeletedFlagFalse(
                testId, grammarVocabTypes).intValue();
        int grammarVocabCorrect = answerRepo.countCorrectByUserAndTestAndQuestionTypes(
                userId, testId, grammarVocabTypes).intValue();
        double grammarVocabScore = calculateSectionScore(grammarVocabTotal, grammarVocabCorrect, totalMax, totalQuestions);
        double grammarVocabMaxScore = grammarVocabTotal > 0 
                ? (double) totalMax * grammarVocabTotal / totalQuestions 
                : 0.0;

        // 2. Reading
        int readingTotal = questionRepo.countByTest_IdAndQuestionTypeAndDeletedFlagFalse(
                testId, JlptQuestionType.READING).intValue();
        int readingCorrect = answerRepo.countCorrectByUserAndTestAndQuestionTypes(
                userId, testId, java.util.List.of(JlptQuestionType.READING)).intValue();
        double readingScore = calculateSectionScore(readingTotal, readingCorrect, totalMax, totalQuestions);
        double readingMaxScore = readingTotal > 0 
                ? (double) totalMax * readingTotal / totalQuestions 
                : 0.0;

        // 3. Listening
        int listeningTotal = questionRepo.countByTest_IdAndQuestionTypeAndDeletedFlagFalse(
                testId, JlptQuestionType.LISTENING).intValue();
        int listeningCorrect = answerRepo.countCorrectByUserAndTestAndQuestionTypes(
                userId, testId, java.util.List.of(JlptQuestionType.LISTENING)).intValue();
        double listeningScore = calculateSectionScore(listeningTotal, listeningCorrect, totalMax, totalQuestions);
        double listeningMaxScore = listeningTotal > 0 
                ? (double) totalMax * listeningTotal / totalQuestions 
                : 0.0;

        // ====== Áp dụng rule JLPT chính thức ======
        String level = test.getLevel();
        double passScore = calculatePassScore(level, totalMax);
        boolean passed = score >= passScore;

        return JlptTestResultResponse.builder()
                .testId(testId)
                .userId(userId)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .score(score)
                .level(level)
                .passScore(passScore)
                .passed(passed)
                .attemptId(null)  // Chưa nộp bài
                .startedAt(null)
                .submittedAt(null)
                .grammarVocab(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(grammarVocabTotal)
                        .correctCount(grammarVocabCorrect)
                        .score(grammarVocabScore)
                        .maxScore(grammarVocabMaxScore)
                        .build())
                .reading(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(readingTotal)
                        .correctCount(readingCorrect)
                        .score(readingScore)
                        .maxScore(readingMaxScore)
                        .build())
                .listening(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(listeningTotal)
                        .correctCount(listeningCorrect)
                        .score(listeningScore)
                        .maxScore(listeningMaxScore)
                        .build())
                .build();
    }

    /**
     * Trả về kết quả rỗng khi user chưa làm bài lần nào.
     */
    private JlptTestResultResponse getEmptyResult(Long testId, Long userId, JlptTest test) {
        int totalQuestions = questionRepo.countByTest_IdAndDeletedFlagFalse(testId).intValue();
        int totalMax = test.getTotalScore() != null ? test.getTotalScore() : 180;
        
        // Điểm từng phần
        java.util.List<JlptQuestionType> grammarVocabTypes = java.util.List.of(
                JlptQuestionType.GRAMMAR,
                JlptQuestionType.VOCAB
        );
        int grammarVocabTotal = questionRepo.countByTest_IdAndQuestionTypeInAndDeletedFlagFalse(
                testId, grammarVocabTypes).intValue();
        int readingTotal = questionRepo.countByTest_IdAndQuestionTypeAndDeletedFlagFalse(
                testId, JlptQuestionType.READING).intValue();
        int listeningTotal = questionRepo.countByTest_IdAndQuestionTypeAndDeletedFlagFalse(
                testId, JlptQuestionType.LISTENING).intValue();

        double grammarVocabMaxScore = grammarVocabTotal > 0 
                ? (double) totalMax * grammarVocabTotal / totalQuestions 
                : 0.0;
        double readingMaxScore = readingTotal > 0 
                ? (double) totalMax * readingTotal / totalQuestions 
                : 0.0;
        double listeningMaxScore = listeningTotal > 0 
                ? (double) totalMax * listeningTotal / totalQuestions 
                : 0.0;

        String level = test.getLevel();
        double passScore = calculatePassScore(level, totalMax);

        return JlptTestResultResponse.builder()
                .testId(testId)
                .userId(userId)
                .totalQuestions(totalQuestions)
                .correctCount(0)
                .score(0.0)
                .level(level)
                .passScore(passScore)
                .passed(false)
                .attemptId(null)
                .startedAt(null)
                .submittedAt(null)
                .grammarVocab(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(grammarVocabTotal)
                        .correctCount(0)
                        .score(0.0)
                        .maxScore(grammarVocabMaxScore)
                        .build())
                .reading(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(readingTotal)
                        .correctCount(0)
                        .score(0.0)
                        .maxScore(readingMaxScore)
                        .build())
                .listening(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(listeningTotal)
                        .correctCount(0)
                        .score(0.0)
                        .maxScore(listeningMaxScore)
                        .build())
                .build();
    }

    /**
     * Convert từ JlptTestAttempt (đã submit) sang JlptTestResultResponse.
     */
    private JlptTestResultResponse convertAttemptToResultResponse(JlptTestAttempt attempt, JlptTest test) {
        int totalMax = test.getTotalScore() != null ? test.getTotalScore() : 180;
        int totalQuestions = attempt.getTotalQuestions() != null ? attempt.getTotalQuestions() : 0;
        
        // Tính maxScore cho từng phần
        double grammarVocabMaxScore = 0.0;
        if (attempt.getGrammarVocabTotal() != null && totalQuestions > 0) {
            grammarVocabMaxScore = (double) totalMax * attempt.getGrammarVocabTotal() / totalQuestions;
        }
        
        double readingMaxScore = 0.0;
        if (attempt.getReadingTotal() != null && totalQuestions > 0) {
            readingMaxScore = (double) totalMax * attempt.getReadingTotal() / totalQuestions;
        }
        
        double listeningMaxScore = 0.0;
        if (attempt.getListeningTotal() != null && totalQuestions > 0) {
            listeningMaxScore = (double) totalMax * attempt.getListeningTotal() / totalQuestions;
        }

        String level = test.getLevel();
        double passScore = calculatePassScore(level, totalMax);

        return JlptTestResultResponse.builder()
                .testId(attempt.getTest().getId())
                .userId(attempt.getUser().getId())
                .totalQuestions(totalQuestions)
                .correctCount(attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0)
                .score(attempt.getScore() != null ? attempt.getScore() : 0.0)
                .level(level)
                .passScore(passScore)
                .passed(attempt.getPassed() != null ? attempt.getPassed() : false)
                .attemptId(attempt.getId())  // Có attemptId vì đã submit
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .grammarVocab(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(attempt.getGrammarVocabTotal() != null ? attempt.getGrammarVocabTotal() : 0)
                        .correctCount(attempt.getGrammarVocabCorrect() != null ? attempt.getGrammarVocabCorrect() : 0)
                        .score(attempt.getGrammarVocabScore() != null ? attempt.getGrammarVocabScore() : 0.0)
                        .maxScore(grammarVocabMaxScore)
                        .build())
                .reading(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(attempt.getReadingTotal() != null ? attempt.getReadingTotal() : 0)
                        .correctCount(attempt.getReadingCorrect() != null ? attempt.getReadingCorrect() : 0)
                        .score(attempt.getReadingScore() != null ? attempt.getReadingScore() : 0.0)
                        .maxScore(readingMaxScore)
                        .build())
                .listening(JlptTestResultResponse.SectionScore.builder()
                        .totalQuestions(attempt.getListeningTotal() != null ? attempt.getListeningTotal() : 0)
                        .correctCount(attempt.getListeningCorrect() != null ? attempt.getListeningCorrect() : 0)
                        .score(attempt.getListeningScore() != null ? attempt.getListeningScore() : 0.0)
                        .maxScore(listeningMaxScore)
                        .build())
                .build();
    }

    /**
     * Tính điểm cho 1 phần thi (Grammar+Vocab, Reading, Listening)
     * 
     * @param sectionTotal Tổng số câu trong phần này
     * @param sectionCorrect Số câu đúng trong phần này
     * @param totalMax Điểm tối đa của toàn bộ test (thường là 180)
     * @param totalQuestions Tổng số câu của toàn bộ test
     * @return Điểm của phần này (tính theo tỷ lệ)
     */
    private double calculateSectionScore(int sectionTotal, int sectionCorrect, int totalMax, int totalQuestions) {
        if (sectionTotal == 0 || totalQuestions == 0 || totalMax == 0) {
            return 0.0;
        }
        // Điểm phần này = (tổng điểm test / tổng số câu) * số câu đúng trong phần này
        // Hoặc: (tổng điểm test * số câu đúng phần này) / tổng số câu
        return (double) totalMax * sectionCorrect / totalQuestions;
    }

    /**
     * Nộp bài và lưu kết quả vào attempt.
     * Sau khi nộp bài, user có thể làm lại test (tạo attempt mới).
     */
    @Transactional
    public JlptTestResultResponse submitTest(Long testId, Long userId) {
        JlptTest test = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Lấy session để biết thời gian bắt đầu
        JlptUserTestSession session = sessionRepo
                .findByTest_IdAndUser_Id(testId, userId)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa start bài thi này"));

        // Cho phép submit ngay cả khi đã hết thời gian (user có thể submit muộn)
        // Nhưng vẫn lưu thời gian submit thực tế

        // Tính kết quả hiện tại
        JlptTestResultResponse result = getResultForUser(testId, userId);

        Instant now = Instant.now();

        // Lấy thông tin điểm từng phần từ result
        JlptTestResultResponse.SectionScore grammarVocab = result.getGrammarVocab();
        JlptTestResultResponse.SectionScore reading = result.getReading();
        JlptTestResultResponse.SectionScore listening = result.getListening();

        // Lưu attempt vào database
        JlptTestAttempt attempt = JlptTestAttempt.builder()
                .user(user)
                .test(test)
                .startedAt(session.getStartedAt())
                .submittedAt(now)
                .totalQuestions(result.getTotalQuestions())
                .correctCount(result.getCorrectCount())
                .score(result.getScore())
                .passed(result.getPassed())
                .grammarVocabTotal(grammarVocab != null ? grammarVocab.getTotalQuestions() : 0)
                .grammarVocabCorrect(grammarVocab != null ? grammarVocab.getCorrectCount() : 0)
                .grammarVocabScore(grammarVocab != null ? grammarVocab.getScore() : 0.0)
                .readingTotal(reading != null ? reading.getTotalQuestions() : 0)
                .readingCorrect(reading != null ? reading.getCorrectCount() : 0)
                .readingScore(reading != null ? reading.getScore() : 0.0)
                .listeningTotal(listening != null ? listening.getTotalQuestions() : 0)
                .listeningCorrect(listening != null ? listening.getCorrectCount() : 0)
                .listeningScore(listening != null ? listening.getScore() : 0.0)
                .build();

        attemptRepo.save(attempt);

        // Lưu chi tiết answers vào attempt trước khi xóa
        List<JlptAnswer> answers = answerRepo.findByUser_IdAndTest_Id(userId, testId);
        for (JlptAnswer answer : answers) {
            // Tìm đáp án đúng của câu hỏi này
            JlptOption correctOption = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(answer.getQuestion().getId())
                    .stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Question " + answer.getQuestion().getId() + " has no correct option"));

            // Lưu vào attempt answer
            JlptTestAttemptAnswer attemptAnswer = JlptTestAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(answer.getQuestion())
                    .selectedOption(answer.getSelectedOption())
                    .correctOption(correctOption)
                    .isCorrect(answer.getIsCorrect())
                    .build();
            attemptAnswerRepo.save(attemptAnswer);
        }

        // Xóa session và answers để user có thể làm lại từ đầu
        sessionRepo.delete(session);
        answerRepo.deleteByUser_IdAndTest_Id(userId, testId);

        // Cập nhật result với thông tin attempt vừa tạo
        return JlptTestResultResponse.builder()
                .testId(result.getTestId())
                .userId(result.getUserId())
                .totalQuestions(result.getTotalQuestions())
                .correctCount(result.getCorrectCount())
                .score(result.getScore())
                .level(result.getLevel())
                .passScore(result.getPassScore())
                .passed(result.getPassed())
                .attemptId(attempt.getId())
                .startedAt(session.getStartedAt())
                .submittedAt(now)
                .grammarVocab(result.getGrammarVocab())
                .reading(result.getReading())
                .listening(result.getListening())
                .build();
    }

    /**
     * Lấy lịch sử các lần làm bài của user cho 1 test.
     * Sắp xếp theo thời gian nộp bài mới nhất trước.
     */
    @Transactional(readOnly = true)
    public List<JlptTestAttemptResponse> getAttemptHistory(Long testId, Long userId) {
        List<JlptTestAttempt> attempts = attemptRepo.findByUser_IdAndTest_IdOrderBySubmittedAtDesc(userId, testId);
        
        return attempts.stream()
                .map(this::mapAttemptToResponse)
                .toList();
    }

    /**
     * Lấy chi tiết một attempt cụ thể, bao gồm đáp án đã chọn và đáp án đúng của từng câu hỏi.
     */
    @Transactional(readOnly = true)
    public JlptTestAttemptDetailResponse getAttemptDetail(Long testId, Long attemptId, Long userId) {
        // Kiểm tra attempt thuộc về user và test
        JlptTestAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        
        if (!attempt.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this attempt");
        }
        
        if (!attempt.getTest().getId().equals(testId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test");
        }

        JlptTest test = attempt.getTest();
        int totalMax = test.getTotalScore() != null ? test.getTotalScore() : 180;
        int totalQuestions = attempt.getTotalQuestions() != null ? attempt.getTotalQuestions() : 0;

        // Tính maxScore cho từng phần
        double grammarVocabMaxScore = 0.0;
        if (attempt.getGrammarVocabTotal() != null && totalQuestions > 0) {
            grammarVocabMaxScore = (double) totalMax * attempt.getGrammarVocabTotal() / totalQuestions;
        }
        double readingMaxScore = 0.0;
        if (attempt.getReadingTotal() != null && totalQuestions > 0) {
            readingMaxScore = (double) totalMax * attempt.getReadingTotal() / totalQuestions;
        }
        double listeningMaxScore = 0.0;
        if (attempt.getListeningTotal() != null && totalQuestions > 0) {
            listeningMaxScore = (double) totalMax * attempt.getListeningTotal() / totalQuestions;
        }

        String level = test.getLevel();
        double passScore = calculatePassScore(level, totalMax);

        // Build summary
        JlptTestAttemptDetailResponse.AttemptSummary summary = JlptTestAttemptDetailResponse.AttemptSummary.builder()
                .attemptId(attempt.getId())
                .testId(attempt.getTest().getId())
                .userId(attempt.getUser().getId())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .totalQuestions(totalQuestions)
                .correctCount(attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0)
                .score(attempt.getScore() != null ? attempt.getScore() : 0.0)
                .level(level)
                .passScore(passScore)
                .passed(attempt.getPassed() != null ? attempt.getPassed() : false)
                .grammarVocab(JlptTestAttemptDetailResponse.SectionScore.builder()
                        .totalQuestions(attempt.getGrammarVocabTotal() != null ? attempt.getGrammarVocabTotal() : 0)
                        .correctCount(attempt.getGrammarVocabCorrect() != null ? attempt.getGrammarVocabCorrect() : 0)
                        .score(attempt.getGrammarVocabScore() != null ? attempt.getGrammarVocabScore() : 0.0)
                        .maxScore(grammarVocabMaxScore)
                        .build())
                .reading(JlptTestAttemptDetailResponse.SectionScore.builder()
                        .totalQuestions(attempt.getReadingTotal() != null ? attempt.getReadingTotal() : 0)
                        .correctCount(attempt.getReadingCorrect() != null ? attempt.getReadingCorrect() : 0)
                        .score(attempt.getReadingScore() != null ? attempt.getReadingScore() : 0.0)
                        .maxScore(readingMaxScore)
                        .build())
                .listening(JlptTestAttemptDetailResponse.SectionScore.builder()
                        .totalQuestions(attempt.getListeningTotal() != null ? attempt.getListeningTotal() : 0)
                        .correctCount(attempt.getListeningCorrect() != null ? attempt.getListeningCorrect() : 0)
                        .score(attempt.getListeningScore() != null ? attempt.getListeningScore() : 0.0)
                        .maxScore(listeningMaxScore)
                        .build())
                .build();

        // Lấy tất cả answers của attempt
        List<JlptTestAttemptAnswer> attemptAnswers = attemptAnswerRepo.findByAttempt_IdOrderByQuestion_OrderIndexAsc(attemptId);
        
        // Build question details
        List<JlptTestAttemptDetailResponse.QuestionDetail> questionDetails = attemptAnswers.stream().map(attemptAnswer -> {
            JlptQuestion question = attemptAnswer.getQuestion();
            JlptOption selectedOption = attemptAnswer.getSelectedOption();
            JlptOption correctOption = attemptAnswer.getCorrectOption();
            
            // Lấy tất cả options của câu hỏi này
            List<JlptOption> allOptions = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(question.getId());
            List<JlptTestAttemptDetailResponse.OptionDetail> optionDetails = allOptions.stream()
                    .map(opt -> JlptTestAttemptDetailResponse.OptionDetail.builder()
                            .optionId(opt.getId())
                            .content(opt.getContent())
                            .imagePath(opt.getImagePath())
                            .imageAltText(opt.getImageAltText())
                            .orderIndex(opt.getOrderIndex())
                            .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                            .build())
                    .toList();

            // Map audioPath thành audioUrl
            String audioUrl = null;
            if (question.getAudioPath() != null && !question.getAudioPath().isEmpty()) {
                audioUrl = "/files" + question.getAudioPath();
            }

            return JlptTestAttemptDetailResponse.QuestionDetail.builder()
                    .questionId(question.getId())
                    .questionContent(question.getContent())
                    .questionType(question.getQuestionType() != null ? question.getQuestionType().name() : null)
                    .explanation(question.getExplanation())
                    .orderIndex(question.getOrderIndex())
                    .audioUrl(audioUrl)
                    .imagePath(question.getImagePath())
                    .imageAltText(question.getImageAltText())
                    .selectedOptionId(selectedOption.getId())
                    .selectedOptionContent(selectedOption.getContent())
                    .selectedOptionImagePath(selectedOption.getImagePath())
                    .correctOptionId(correctOption.getId())
                    .correctOptionContent(correctOption.getContent())
                    .correctOptionImagePath(correctOption.getImagePath())
                    .isCorrect(attemptAnswer.getIsCorrect())
                    .allOptions(optionDetails)
                    .build();
        }).toList();

        return JlptTestAttemptDetailResponse.builder()
                .summary(summary)
                .questions(questionDetails)
                .build();
    }

    /**
     * Map entity JlptTestAttempt sang DTO response.
     */
    private JlptTestAttemptResponse mapAttemptToResponse(JlptTestAttempt attempt) {
        return JlptTestAttemptResponse.builder()
                .id(attempt.getId())
                .testId(attempt.getTest().getId())
                .userId(attempt.getUser().getId())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .totalQuestions(attempt.getTotalQuestions())
                .correctCount(attempt.getCorrectCount())
                .score(attempt.getScore())
                .passed(attempt.getPassed())
                .grammarVocab(JlptTestAttemptResponse.SectionScore.builder()
                        .totalQuestions(attempt.getGrammarVocabTotal())
                        .correctCount(attempt.getGrammarVocabCorrect())
                        .score(attempt.getGrammarVocabScore())
                        .build())
                .reading(JlptTestAttemptResponse.SectionScore.builder()
                        .totalQuestions(attempt.getReadingTotal())
                        .correctCount(attempt.getReadingCorrect())
                        .score(attempt.getReadingScore())
                        .build())
                .listening(JlptTestAttemptResponse.SectionScore.builder()
                        .totalQuestions(attempt.getListeningTotal())
                        .correctCount(attempt.getListeningCorrect())
                        .score(attempt.getListeningScore())
                        .build())
                .build();
    }

    /**
     * Tính điểm cần thiết để đậu theo level JLPT.
     *
     * Official (trên thang 180 điểm):
     *  - N5: >= 80 / 180
     *  - N4: >= 90 / 180
     *  - N3: >= 95 / 180
     *  - N2: >= 90 / 180
     *  - N1: >= 100 / 180
     *
     * Nếu đề của em không phải đúng 180 điểm thì hàm này sẽ scale
     * theo tổng điểm `totalMax` của đề.
     */
    private double calculatePassScore(String level, int totalMax) {
        double ratio;   // tỉ lệ pass trên thang điểm 1.0

        if (level == null) {
            // không biết level thì coi như 0, luôn đậu
            return 0.0;
        }

        switch (level.toUpperCase()) {
            case "N5" -> ratio = 80.0 / 180.0;
            case "N4" -> ratio = 90.0 / 180.0;
            case "N3" -> ratio = 95.0 / 180.0;
            case "N2" -> ratio = 90.0 / 180.0;
            case "N1" -> ratio = 100.0 / 180.0;
            default   -> ratio = 0.0;  // level lạ -> không bắt buộc pass
        }

        double raw = totalMax * ratio;
        return Math.round(raw * 10.0) / 10.0;
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

        // Validate audioPath exists in file_storage if provided
        if (req.getAudioPath() != null && !req.getAudioPath().isEmpty()) {
            // Remove /files/ prefix if present (filePath should be relative path)
            String filePath = req.getAudioPath().startsWith("/files/")
                    ? req.getAudioPath().substring("/files/".length())
                    : req.getAudioPath();

            if (fileStorageService.getFile(filePath) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audio file not found in storage: " + filePath + ". Please upload the file first.");
            }
        }

        // Validate imagePath exists in file_storage if provided
        if (req.getImagePath() != null && !req.getImagePath().isEmpty()) {
            String filePath = req.getImagePath().startsWith("/files/")
                    ? req.getImagePath().substring("/files/".length())
                    : req.getImagePath();

            if (fileStorageService.getFile(filePath) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image file not found in storage: " + filePath + ". Please upload the file first.");
            }
        }

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

        java.time.Instant now = java.time.Instant.now();
        int durationMin = test.getDurationMin() != null ? test.getDurationMin() : 0;
        java.time.Instant expiresAt = now.plus(java.time.Duration.ofMinutes(durationMin));

        // ===== SESSION MANAGEMENT =====
        // Logic: Tinh thần tự học - giữ progress khi user refresh/out ra rồi vào lại
        // Chỉ xóa answers khi:
        // 1. Lần đầu tiên start test
        // 2. Session đã hết hạn (quá thời gian làm bài)
        
        JlptUserTestSession session = sessionRepo
                .findByTest_IdAndUser_Id(testId, userId)
                .orElse(null);

        // Check if session exists and if it's expired
        session = sessionRepo
                .findByTest_IdAndUser_Id(testId, userId)
                .orElse(null);

        boolean shouldDeleteAnswers = false;
        boolean isNewSession = (session == null);
        boolean isExpired = (session != null && now.isAfter(session.getExpiresAt()));

        if (isNewSession || isExpired) {
            // New session or expired session → reset and delete answers
            shouldDeleteAnswers = true;
            
            // Use atomic upsert to handle race conditions
            sessionRepo.upsertSession(testId, userId, now, expiresAt, now);
            
            // Reload session after upsert
            session = sessionRepo
                    .findByTest_IdAndUser_Id(testId, userId)
                    .orElseThrow(() -> new RuntimeException("Failed to create/update session"));

            // Only increment participants for new sessions (first time user takes this test)
            if (isNewSession) {
                Integer cur = test.getCurrentParticipants();
                test.setCurrentParticipants((cur == null ? 0 : cur) + 1);
            }
        } else {
            // Session còn valid (user refresh/out ra rồi vào lại trong thời gian làm bài)
            // → Giữ answers để user tiếp tục làm bài (tinh thần tự học)
            // → KHÔNG reset thời gian (giữ nguyên expiresAt để tránh abuse)
            // User chỉ có thể làm bài trong thời gian đã định sẵn
        }

        // Xóa answers nếu cần (chỉ khi lần đầu hoặc session đã hết hạn)
        if (shouldDeleteAnswers) {
            answerRepo.deleteByUser_IdAndTest_Id(userId, testId);
        }

        // 3. Lấy câu hỏi + options (bao gồm selectedOptionId nếu user đã chọn)
        List<JlptQuestionWithOptionsResponse> questions = getQuestionsWithOptions(testId, userId);

        learnerProgressService.recordLearningActivity(userId, now);

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


    @Transactional(readOnly = true)
    public List<JlptTestListItemResponse> listTestsForLearner(Long eventId) {
        List<JlptTest> tests =
                testRepo.findByEvent_IdAndDeletedFlagFalseOrderByCreatedAtDesc(eventId);

        return tests.stream()
                .map(t -> {
                    double passScore = calculatePassScore(
                            t.getLevel(),
                            t.getTotalScore() != null ? t.getTotalScore() : 180
                    );

                    // Ví dụ đặt title tự generate cho learner
                    String title = "JLPT " + t.getLevel() + " – Mock Test #" + t.getId();

                    return JlptTestListItemResponse.builder()
                            .id(t.getId())
                            .title(title)
                            .level(t.getLevel())
                            .durationMin(t.getDurationMin())
                            .totalScore(t.getTotalScore())
                            .passScore(passScore)
                            .currentParticipants(t.getCurrentParticipants())
                            .build();
                })
                .toList();
    }


    @Transactional(readOnly = true)
    public long getActiveUserCount(Long testId) {
        Instant now = Instant.now();
        return sessionRepo.countByTest_IdAndExpiresAtAfter(testId, now);
    }



}
