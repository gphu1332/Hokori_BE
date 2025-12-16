package com.hokori.web.service;

import com.hokori.web.dto.quiz.*;
import com.hokori.web.entity.*;
import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LearnerQuizService {

    private final QuizRepository quizRepo;
    private final LessonRepository lessonRepo;
    private final EnrollmentRepository enrollRepo;
    private final ChapterRepository chapterRepo; // For trial chapter check
    private final QuizAttemptRepository attemptRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final LearnerProgressService learnerProgressService;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;
    private final UserContentProgressRepository ucpRepo; // To check if content already completed
    private final UserRepository userRepo; // For loading User entity for QuizAttempt

    /**
     * Helper method to check enrollment and get courseId from sectionId.
     * Allows access if enrolled OR if section's lesson belongs to trial chapter.
     */
    private Long checkEnrollmentAndGetCourseId(Long sectionId, Long userId) {
        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found"));
        
        // Get section to check trial chapter
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found"));
        
        Lesson lesson = section.getLesson();
        if (lesson == null) {
            throw new EntityNotFoundException("Lesson not found for section");
        }
        
        Chapter chapter = lesson.getChapter();
        if (chapter == null) {
            throw new EntityNotFoundException("Chapter not found for lesson");
        }
        
        boolean isTrialChapter = chapter.isTrial();
        
        // If not trial chapter, require enrollment
        if (!isTrialChapter) {
            if (!enrollRepo.existsByUserIdAndCourseId(userId, courseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You must enroll in this course before taking the quiz");
            }
        }
        // If trial chapter, allow access without enrollment
        
        return courseId;
    }

    public AttemptDto startAttempt(Long sectionId, Long userId, StartAttemptReq req) {
        // Check enrollment first
        checkEnrollmentAndGetCourseId(sectionId, userId);
        
        // Use native query to avoid LOB stream error
        var quizMetadataOpt = quizRepo.findQuizMetadataBySectionId(sectionId);
        if (quizMetadataOpt.isEmpty()) {
            throw new EntityNotFoundException("Quiz not found for section " + sectionId);
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        Long quizId = ((Number) actualQMeta[0]).longValue();
        String quizTitle = actualQMeta[2] != null ? actualQMeta[2].toString() : null;
        
        // Get Quiz entity reference for QuizAttempt (only need ID)
        Quiz quizRef = quizRepo.getReferenceById(quizId);
        
        // Get User entity reference for QuizAttempt
        User userRef = userRepo.getReferenceById(userId);

        if (req == null || !Boolean.TRUE.equals(req.forceNew())) {
            var last = attemptRepo.findByUserIdAndQuiz_IdOrderByStartedAtDesc(userId, quizId);
            if (!last.isEmpty() && last.get(0).getStatus() == QuizAttempt.Status.IN_PROGRESS) {
                return toDto(last.get(0), quizTitle);
            }
        }

        QuizAttempt a = new QuizAttempt();
        a.setUser(userRef); // Set User entity thay vì chỉ userId
        a.setQuiz(quizRef);
        a.setTotalQuestions(attemptRepo.countQuestions(quizId));
        attemptRepo.save(a);

        learnerProgressService.recordLearningActivity(userId, java.time.Instant.now());

        return toDto(a, quizTitle);
    }

    @Transactional(readOnly = true)
    public PlayQuestionDto nextQuestion(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        
        // Check enrollment via quiz's section
        Quiz quiz = a.getQuiz();
        if (quiz.getSection() != null) {
            checkEnrollmentAndGetCourseId(quiz.getSection().getId(), userId);
        }
        
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt not in progress");

        // Use native query to avoid LOB stream error
        var unansweredIds = answerRepo.findUnansweredQuestionIds(a.getQuiz().getId(), a.getId());
        if (unansweredIds.isEmpty()) return null;

        Long questionId = unansweredIds.get(0);
        
        // Get question metadata using native query
        var questionMetadataOpt = questionRepo.findQuestionMetadataById(questionId);
        if (questionMetadataOpt.isEmpty()) {
            throw new EntityNotFoundException("Question not found");
        }
        
        Object[] qMeta = questionMetadataOpt.get();
        // Handle nested array case (PostgreSQL)
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Metadata: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
        Long qId = ((Number) actualQMeta[0]).longValue();
        String content = actualQMeta[2] != null ? actualQMeta[2].toString() : null;
        String questionType = actualQMeta[3] != null ? actualQMeta[3].toString() : null;
        Integer orderIndex = actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null;

        // Get options metadata using native query
        var optsMeta = optionRepo.findOptionMetadataByQuestionId(questionId);
        List<PlayOptionDto> opts = optsMeta.stream()
                .map(optMeta -> {
                    // Handle nested array case
                    Object[] actualOptMeta = optMeta;
                    if (optMeta.length == 1 && optMeta[0] instanceof Object[]) {
                        actualOptMeta = (Object[]) optMeta[0];
                    }
                    // Metadata: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
                    Long optId = ((Number) actualOptMeta[0]).longValue();
                    String optContent = actualOptMeta[2] != null ? actualOptMeta[2].toString() : null;
                    return new PlayOptionDto(optId, optContent);
                })
                .toList();

        return new PlayQuestionDto(
                qId,
                content,
                questionType,
                orderIndex,
                opts
        );
    }

    public void answer(Long attemptId, Long userId, Long questionId, AnswerReq req) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        
        // Check enrollment via quiz's section
        Quiz quiz = a.getQuiz();
        Section section = quiz.getSection();
        if (section != null) {
            checkEnrollmentAndGetCourseId(section.getId(), userId);
        }
        
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt not in progress");

        Question q = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));
        if (!Objects.equals(q.getQuiz().getId(), a.getQuiz().getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not in this quiz");

        // AnswerReq là record → dùng optionId()
        Option chosen = optionRepo.findById(req.optionId())
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));
        if (!Objects.equals(chosen.getQuestion().getId(), q.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option not in this question");

        QuizAnswer ans = answerRepo.findOne(a.getId(), q.getId()).orElse(new QuizAnswer());
        ans.setAttempt(a);
        ans.setQuestion(q);
        ans.setOption(chosen);
        answerRepo.save(ans);
    }

    public AttemptDto submit(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        
        // Check enrollment via quiz's section
        Quiz quiz = a.getQuiz();
        Section section = quiz.getSection();
        if (section != null) {
            checkEnrollmentAndGetCourseId(section.getId(), userId);
        }
        
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt already submitted");

        int total = (a.getTotalQuestions() != null)
                ? a.getTotalQuestions()
                : attemptRepo.countQuestions(a.getQuiz().getId());

        int correct = 0;
        // Load answers with Option to avoid LazyInitializationException
        var answers = answerRepo.findByAttempt_IdWithOption(a.getId());
        for (QuizAnswer ans : answers) {
            // Option may be null if user didn't answer this question
            if (ans.getOption() != null) {
                boolean ok = Boolean.TRUE.equals(ans.getOption().getIsCorrect());
                ans.setIsCorrect(ok);
                if (ok) correct++;
            } else {
                // No answer provided - mark as incorrect
                ans.setIsCorrect(false);
            }
        }
        answerRepo.saveAll(answers);

        int score = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
        a.setCorrectCount(correct);
        a.setTotalQuestions(total);
        a.setScorePercent(score);
        a.setSubmittedAt(LocalDateTime.now());
        a.setStatus(QuizAttempt.Status.SUBMITTED);
        attemptRepo.save(a);

        // Auto-mark last trackable content in section as completed when quiz is submitted AND passed
        // This makes quiz completion count towards progress % only if learner achieves pass score
        // Only mark if this is the latest attempt (to avoid marking based on old attempts)
        if (section != null) {
            try {
                markLastContentCompleteForQuiz(section.getId(), userId, quiz, score, a.getId());
            } catch (Exception e) {
                // Log error but don't fail quiz submission
                // Quiz submission should succeed even if progress update fails
                System.err.println("Failed to mark content complete after quiz submission: " + e.getMessage());
            }
        }

        // Get passScorePercent from quiz to include in response
        Integer passScorePercent = quiz.getPassScorePercent();
        Boolean passed = null;
        if (passScorePercent != null) {
            passed = score >= passScorePercent;
        } else {
            // If no passScorePercent, consider passed if score > 0
            passed = score > 0;
        }
        
        return toDto(a, a.getQuiz().getTitle(), passScorePercent, passed);
    }
    
    /**
     * Mark the last trackable content in section as completed when quiz is submitted AND passed.
     * This makes quiz completion count towards progress percentage only if learner achieves pass score.
     * Only marks if this is the latest submitted attempt (to avoid marking based on old attempts).
     * 
     * @param sectionId The section ID
     * @param userId The user ID
     * @param quiz The quiz entity (to check passScorePercent)
     * @param score The score achieved by the learner (0-100)
     * @param attemptId The current attempt ID (to check if it's the latest)
     */
    private void markLastContentCompleteForQuiz(Long sectionId, Long userId, Quiz quiz, int score, Long attemptId) {
        // Check if this is the highest scoring attempt
        // Only mark content completed based on the highest scoring attempt to ensure progress reflects best performance
        var allAttempts = attemptRepo.findByUserIdAndQuiz_IdOrderByStartedAtDesc(userId, quiz.getId());
        if (!allAttempts.isEmpty()) {
            QuizAttempt highestScoreAttempt = allAttempts.stream()
                    .filter(att -> att.getStatus() == QuizAttempt.Status.SUBMITTED 
                            && att.getScorePercent() != null)
                    .max((a1, a2) -> {
                        // Compare by score (higher is better)
                        int score1 = a1.getScorePercent() != null ? a1.getScorePercent() : 0;
                        int score2 = a2.getScorePercent() != null ? a2.getScorePercent() : 0;
                        int scoreCompare = Integer.compare(score1, score2);
                        
                        // If scores are equal, prefer the latest attempt
                        if (scoreCompare == 0) {
                            if (a1.getSubmittedAt() == null && a2.getSubmittedAt() == null) return 0;
                            if (a1.getSubmittedAt() == null) return -1;
                            if (a2.getSubmittedAt() == null) return 1;
                            return a1.getSubmittedAt().compareTo(a2.getSubmittedAt());
                        }
                        
                        return scoreCompare;
                    })
                    .orElse(null);
            
            // Only proceed if current attempt is the highest scoring attempt
            if (highestScoreAttempt == null || !highestScoreAttempt.getId().equals(attemptId)) {
                return; // Not the highest scoring attempt, skip marking
            }
        }
        // Check if quiz has pass score requirement
        Integer passScorePercent = quiz.getPassScorePercent();
        
        // If pass score is set, learner must achieve it to count towards progress
        if (passScorePercent != null) {
            if (score < passScorePercent) {
                // Learner didn't pass, don't mark content as completed
                return;
            }
            // Score >= passScorePercent, proceed to mark content complete
        } else {
            // If passScorePercent is null, teacher hasn't set a pass requirement
            // In this case, we still require a minimum score > 0 to mark as completed
            // This prevents marking content complete when score = 0% (no answers correct)
            if (score <= 0) {
                // Score is 0% or negative, don't mark content as completed
                return;
            }
            // Score > 0, proceed to mark content complete
        }
        // Get section and check if it belongs to trial chapter
        Section section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found"));
        
        Lesson lesson = section.getLesson();
        if (lesson == null) {
            throw new EntityNotFoundException("Lesson not found for section");
        }
        
        Chapter chapter = lesson.getChapter();
        if (chapter == null) {
            throw new EntityNotFoundException("Chapter not found for lesson");
        }
        
        Long courseId = chapter.getCourse().getId();
        
        // Skip if trial chapter (trial chapters don't count towards progress)
        if (Boolean.TRUE.equals(chapter.isTrial())) {
            return;
        }
        
        // Check enrollment
        if (!enrollRepo.existsByUserIdAndCourseId(userId, courseId)) {
            return; // Not enrolled, skip
        }
        
        // Find the QUIZ content format for this quiz (should be exactly one)
        // This is the content that was auto-created when quiz was created
        List<SectionsContent> quizContents = contentRepo.findBySection_IdOrderByOrderIndexAsc(sectionId).stream()
                .filter(c -> c.getContentFormat() == ContentFormat.QUIZ 
                        && c.getQuizId() != null 
                        && c.getQuizId().equals(quiz.getId())
                        && Boolean.TRUE.equals(c.getIsTrackable()))
                .collect(java.util.stream.Collectors.toList());
        
        // Mark the QUIZ content as completed
        // Only mark if content is not already completed (one-time mark based on highest score)
        if (!quizContents.isEmpty()) {
            SectionsContent quizContent = quizContents.get(0);
            Long contentId = quizContent.getId();
            
            // Check if content is already completed
            // If already completed, skip marking (one-time completion based on highest score attempt)
            Enrollment enrollment = enrollRepo.findLatestByUserIdAndCourseId(userId, courseId)
                    .orElse(null);
            if (enrollment != null) {
                var existingProgress = ucpRepo.findByEnrollment_IdAndContent_Id(enrollment.getId(), contentId);
                if (existingProgress.isPresent() && Boolean.TRUE.equals(existingProgress.get().getIsCompleted())) {
                    // Content already completed, skip marking
                    // Subsequent attempts are only for display purposes
                    return;
                }
            }
            
            // Use LearnerProgressService to update content progress
            // This will automatically recompute course percent
            com.hokori.web.dto.progress.ContentProgressUpsertReq req = 
                    new com.hokori.web.dto.progress.ContentProgressUpsertReq();
            req.setIsCompleted(true);
            
            learnerProgressService.updateContentProgress(userId, contentId, req);
        }
    }

    @Transactional(readOnly = true)
    public AttemptDetailDto detail(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        
        // Check enrollment via quiz's section
        Quiz quiz = a.getQuiz();
        Section section = quiz.getSection();
        if (section != null) {
            checkEnrollmentAndGetCourseId(section.getId(), userId);
        }

        // Load answers with Option and Question to avoid LazyInitializationException
        var answers = answerRepo.findByAttempt_IdWithOption(a.getId());
        Map<Long, QuizAnswer> map = new HashMap<>();
        for (QuizAnswer x : answers) {
            // Access question.id to trigger lazy loading while in transaction
            if (x.getQuestion() != null) {
                map.put(x.getQuestion().getId(), x);
            }
        }

        var questions = questionRepo.findByQuiz_IdOrderByOrderIndexAsc(a.getQuiz().getId());

        var items = questions.stream().map(q -> {
            QuizAnswer ans = map.get(q.getId());
            Long chosenId = (ans != null && ans.getOption() != null) ? ans.getOption().getId() : null;

            Long correctId = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId())
                    .stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(Option::getId)
                    .findFirst().orElse(null);

            Boolean isCorrect = (ans != null) ? ans.getIsCorrect() : null;
            return new AttemptDetailDto.Item(q.getId(), q.getContent(), chosenId, correctId, isCorrect);
        }).toList();

        // Get passScorePercent and calculate passed for detail view
        Integer passScorePercent = quiz.getPassScorePercent();
        Boolean passed = null;
        if (passScorePercent != null && a.getScorePercent() != null) {
            passed = a.getScorePercent() >= passScorePercent;
        } else if (passScorePercent == null && a.getScorePercent() != null) {
            passed = a.getScorePercent() > 0;
        }
        
        return new AttemptDetailDto(toDto(a, a.getQuiz().getTitle(), passScorePercent, passed), items);
    }

    @Transactional(readOnly = true)
    public List<AttemptDto> history(Long sectionId, Long userId) {
        // Check enrollment first
        checkEnrollmentAndGetCourseId(sectionId, userId);
        
        Quiz quiz = quizRepo.findBySection_Id(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));
        Integer passScorePercent = quiz.getPassScorePercent();
        
        return attemptRepo.findByUserIdAndQuiz_IdOrderByStartedAtDesc(userId, quiz.getId())
                .stream()
                .map(x -> {
                    Boolean passed = null;
                    if (passScorePercent != null && x.getScorePercent() != null) {
                        passed = x.getScorePercent() >= passScorePercent;
                    } else if (passScorePercent == null && x.getScorePercent() != null) {
                        passed = x.getScorePercent() > 0;
                    }
                    return toDto(x, quiz.getTitle(), passScorePercent, passed);
                })
                .toList();
    }
    
    /**
     * Get quiz info (metadata) for a section - requires enrollment
     */
    @Transactional(readOnly = true)
    public com.hokori.web.dto.quiz.QuizInfoDto getQuizInfo(Long sectionId, Long userId) {
        // Check enrollment first
        checkEnrollmentAndGetCourseId(sectionId, userId);
        
        // Use native query to avoid LOB stream error
        var quizMetadataOpt = quizRepo.findQuizMetadataBySectionId(sectionId);
        if (quizMetadataOpt.isEmpty()) {
            throw new EntityNotFoundException("Quiz not found for section " + sectionId);
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Metadata: [id, sectionId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
        Long quizId = ((Number) actualQMeta[0]).longValue();
        String title = actualQMeta[2] != null ? actualQMeta[2].toString() : null;
        String description = actualQMeta[3] != null ? actualQMeta[3].toString() : null;
        Integer totalQuestions = actualQMeta[4] != null ? ((Number) actualQMeta[4]).intValue() : null;
        Integer timeLimitSec = actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null;
        Integer passScorePercent = actualQMeta[6] != null ? ((Number) actualQMeta[6]).intValue() : null;
        
        // Get attempt history count
        long attemptCount = attemptRepo.countByUserIdAndQuiz_Id(userId, quizId);
        
        return new com.hokori.web.dto.quiz.QuizInfoDto(
                quizId,
                title,
                description,
                totalQuestions,
                timeLimitSec,
                passScorePercent,
                attemptCount
        );
    }

    private AttemptDto toDto(QuizAttempt a, String quizTitle) {
        // Default: no passScorePercent info (for backward compatibility)
        return toDto(a, quizTitle, null, null);
    }
    
    private AttemptDto toDto(QuizAttempt a, String quizTitle, Integer passScorePercent, Boolean passed) {
        return new AttemptDto(
                a.getId(),
                a.getQuiz().getId(),
                quizTitle,
                a.getStatus().name(),
                a.getTotalQuestions(),
                a.getCorrectCount(),
                a.getScorePercent(),
                passScorePercent,
                passed,
                a.getStartedAt(),
                a.getSubmittedAt()
        );
    }

    public record AttemptDetailDto(AttemptDto attempt, java.util.List<Item> items) {
        public record Item(Long questionId, String content, Long chosenOptionId, Long correctOptionId, Boolean isCorrect) {}
    }
}
