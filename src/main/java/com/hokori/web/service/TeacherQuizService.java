package com.hokori.web.service;

import com.hokori.web.dto.quiz.*;
import com.hokori.web.entity.Lesson;
import com.hokori.web.entity.Option;
import com.hokori.web.entity.Question;
import com.hokori.web.entity.Quiz;
import com.hokori.web.mapper.TeacherQuizMapper;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TeacherQuizService {

    private final CurrentUserService current;
    private final LessonRepository lessonRepo;
    private final QuizRepository quizRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final TeacherQuizMapper mapper;
    private final CourseService courseService;

    /* ---------- Helpers ---------- */

    private void requireOwner(Long lessonId){
        // Allow moderator if course is pending approval
        if (current.hasRole("MODERATOR")) {
            courseService.requireLessonBelongsToPendingApprovalCourse(lessonId);
            return;
        }
        
        Long me = current.getCurrentUserOrThrow().getId();
        Long owner = lessonRepo.findCourseOwnerIdByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
        if (!owner.equals(me)) throw new RuntimeException("You are not the owner of this lesson/course");
    }

    private Quiz getQuizOrThrow(Long quizId){
        return quizRepo.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    private void refreshTotalQuestions(Quiz quiz){
        int count = (int) questionRepo.countByQuiz_Id(quiz.getId());
        quiz.setTotalQuestions(count);
        quizRepo.save(quiz);
    }

    /* ---------- Quiz ---------- */

    public QuizDto getQuizByLesson(Long lessonId){
        requireOwner(lessonId);
        // Use native query to avoid LOB stream error
        var quizMetadataOpt = quizRepo.findQuizMetadataByLessonId(lessonId);
        if (quizMetadataOpt.isEmpty()) {
            throw new RuntimeException("Quiz not found for this lesson");
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        // Handle nested array case (PostgreSQL)
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Metadata: [id, lessonId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
        return new QuizDto(
                ((Number) actualQMeta[0]).longValue(),
                ((Number) actualQMeta[1]).longValue(),
                actualQMeta[2] != null ? actualQMeta[2].toString() : null,
                actualQMeta[3] != null ? actualQMeta[3].toString() : null,
                actualQMeta[4] != null ? ((Number) actualQMeta[4]).intValue() : null,
                actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null,
                actualQMeta[6] != null ? ((Number) actualQMeta[6]).intValue() : null
        );
    }

    public QuizDto createQuiz(Long lessonId, QuizUpsertReq req){
        requireOwner(lessonId);

        quizRepo.findByLesson_Id(lessonId).ifPresent(q -> {
            throw new RuntimeException("Quiz for this lesson already exists");
        });

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));

        Quiz q = new Quiz();
        q.setLesson(lesson);
        q.setTitle(req.title());
        q.setDescription(req.description());
        q.setTimeLimitSec(req.timeLimitSec());
        q.setPassScorePercent(req.passScorePercent());
        q.setTotalQuestions(0);

        Quiz saved = quizRepo.save(q);
        
        // Use native query to avoid LOB stream error when returning response
        var quizMetadataOpt = quizRepo.findQuizMetadataById(saved.getId());
        if (quizMetadataOpt.isEmpty()) {
            throw new RuntimeException("Failed to retrieve created quiz");
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Metadata: [id, lessonId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
        return new QuizDto(
                ((Number) actualQMeta[0]).longValue(),
                ((Number) actualQMeta[1]).longValue(),
                actualQMeta[2] != null ? actualQMeta[2].toString() : null,
                actualQMeta[3] != null ? actualQMeta[3].toString() : null,
                actualQMeta[4] != null ? ((Number) actualQMeta[4]).intValue() : null,
                actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null,
                actualQMeta[6] != null ? ((Number) actualQMeta[6]).intValue() : null
        );
    }

    public QuizDto updateQuiz(Long lessonId, Long quizId, QuizUpsertReq req){
        requireOwner(lessonId);
        Quiz q = getQuizOrThrow(quizId);
        if (!q.getLesson().getId().equals(lessonId))
            throw new RuntimeException("Quiz doesn't belong to this lesson");

        if (req.title() != null) q.setTitle(req.title());
        if (req.description() != null) q.setDescription(req.description());
        if (req.timeLimitSec() != null) q.setTimeLimitSec(req.timeLimitSec());
        if (req.passScorePercent() != null) q.setPassScorePercent(req.passScorePercent());

        quizRepo.save(q);
        
        // Use native query to avoid LOB stream error when returning response
        var quizMetadataOpt = quizRepo.findQuizMetadataById(quizId);
        if (quizMetadataOpt.isEmpty()) {
            throw new RuntimeException("Failed to retrieve updated quiz");
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Metadata: [id, lessonId, title, description, totalQuestions, timeLimitSec, passScorePercent, createdAt, updatedAt, deletedFlag]
        return new QuizDto(
                ((Number) actualQMeta[0]).longValue(),
                ((Number) actualQMeta[1]).longValue(),
                actualQMeta[2] != null ? actualQMeta[2].toString() : null,
                actualQMeta[3] != null ? actualQMeta[3].toString() : null,
                actualQMeta[4] != null ? ((Number) actualQMeta[4]).intValue() : null,
                actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null,
                actualQMeta[6] != null ? ((Number) actualQMeta[6]).intValue() : null
        );
    }

    /* ---------- Questions ---------- */

    public List<QuestionWithOptionsDto> listQuestions(Long lessonId, Long quizId){
        requireOwner(lessonId);
        // Check quiz belongs to lesson using native query
        var quizMetadataOpt = quizRepo.findQuizMetadataById(quizId);
        if (quizMetadataOpt.isEmpty()) {
            throw new RuntimeException("Quiz not found");
        }
        
        Object[] qMeta = quizMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        Long lessonIdFromQuiz = ((Number) actualQMeta[1]).longValue();
        if (!lessonIdFromQuiz.equals(lessonId)) {
            throw new RuntimeException("Quiz doesn't belong to this lesson");
        }

        // Use native query to avoid LOB stream error
        var questionsMeta = questionRepo.findQuestionMetadataByQuizId(quizId);
        return questionsMeta.stream()
                .map(qMetaArray -> {
                    // Handle nested array case
                    Object[] actualQMetaArray = qMetaArray;
                    if (qMetaArray.length == 1 && qMetaArray[0] instanceof Object[]) {
                        actualQMetaArray = (Object[]) qMetaArray[0];
                    }
                    
                    // Question metadata: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
                    Long qId = ((Number) actualQMetaArray[0]).longValue();
                    String content = actualQMetaArray[2] != null ? actualQMetaArray[2].toString() : null;
                    String questionType = actualQMetaArray[3] != null ? actualQMetaArray[3].toString() : null;
                    String explanation = actualQMetaArray[4] != null ? actualQMetaArray[4].toString() : null;
                    Integer orderIndex = actualQMetaArray[5] != null ? ((Number) actualQMetaArray[5]).intValue() : null;
                    
                    // Get options metadata
                    var optsMeta = optionRepo.findOptionMetadataByQuestionId(qId);
                    List<OptionDto> options = optsMeta.stream()
                            .map(optMetaArray -> {
                                Object[] actualOptMetaArray = optMetaArray;
                                if (optMetaArray.length == 1 && optMetaArray[0] instanceof Object[]) {
                                    actualOptMetaArray = (Object[]) optMetaArray[0];
                                }
                                // Option metadata: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
                                Long optId = ((Number) actualOptMetaArray[0]).longValue();
                                String optContent = actualOptMetaArray[2] != null ? actualOptMetaArray[2].toString() : null;
                                Boolean isCorrect = actualOptMetaArray[3] != null ? 
                                    (actualOptMetaArray[3] instanceof Boolean ? (Boolean) actualOptMetaArray[3] :
                                     ((Number) actualOptMetaArray[3]).intValue() != 0) : false;
                                Integer optOrderIndex = actualOptMetaArray[4] != null ? ((Number) actualOptMetaArray[4]).intValue() : null;
                                return new OptionDto(optId, optContent, isCorrect, optOrderIndex);
                            })
                            .toList();
                    
                    return new QuestionWithOptionsDto(qId, content, explanation, questionType, orderIndex, options);
                })
                .toList();
    }


    public QuestionWithOptionsDto createQuestion(Long lessonId, Long quizId, QuestionUpsertReq req){
        requireOwner(lessonId);
        Quiz quiz = getQuizOrThrow(quizId);
        if (!quiz.getLesson().getId().equals(lessonId))
            throw new RuntimeException("Quiz doesn't belong to this lesson");

        Question qu = new Question();
        qu.setQuiz(quiz);
        qu.setContent(req.content());
        qu.setExplanation(req.explanation());
        qu.setQuestionType(req.questionType() == null ? "SINGLE_CHOICE" : req.questionType());
        int nextOrder = (int) (questionRepo.countByQuiz_Id(quizId) + 1);
        qu.setOrderIndex(req.orderIndex() == null ? nextOrder : req.orderIndex());

        Question saved = questionRepo.save(qu);
        refreshTotalQuestions(quiz);

        // Use native query to avoid LOB stream error when returning response
        var questionMetadataOpt = questionRepo.findQuestionMetadataById(saved.getId());
        if (questionMetadataOpt.isEmpty()) {
            throw new RuntimeException("Failed to retrieve created question");
        }
        
        Object[] qMeta = questionMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Question metadata: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
        Long qId = ((Number) actualQMeta[0]).longValue();
        String content = actualQMeta[2] != null ? actualQMeta[2].toString() : null;
        String questionType = actualQMeta[3] != null ? actualQMeta[3].toString() : null;
        String explanation = actualQMeta[4] != null ? actualQMeta[4].toString() : null;
        Integer orderIndex = actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null;
        
        // Options are empty for new question
        return new QuestionWithOptionsDto(qId, content, explanation, questionType, orderIndex, List.of());
    }

    public QuestionWithOptionsDto updateQuestion(Long lessonId, Long questionId, QuestionUpsertReq req){
        Question qu = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        Long lessonIdFromDb = qu.getQuiz().getLesson().getId();
        requireOwner(lessonIdFromDb);

        if (req.content() != null) qu.setContent(req.content());
        if (req.explanation() != null) qu.setExplanation(req.explanation());
        if (req.questionType() != null) qu.setQuestionType(req.questionType());
        if (req.orderIndex() != null) qu.setOrderIndex(req.orderIndex());

        questionRepo.save(qu);
        
        // Use native query to avoid LOB stream error when returning response
        var questionMetadataOpt = questionRepo.findQuestionMetadataById(questionId);
        if (questionMetadataOpt.isEmpty()) {
            throw new RuntimeException("Failed to retrieve updated question");
        }
        
        Object[] qMeta = questionMetadataOpt.get();
        Object[] actualQMeta = qMeta;
        if (qMeta.length == 1 && qMeta[0] instanceof Object[]) {
            actualQMeta = (Object[]) qMeta[0];
        }
        
        // Question metadata: [id, quizId, content, questionType, explanation, orderIndex, createdAt, updatedAt, deletedFlag]
        Long qId = ((Number) actualQMeta[0]).longValue();
        String content = actualQMeta[2] != null ? actualQMeta[2].toString() : null;
        String questionType = actualQMeta[3] != null ? actualQMeta[3].toString() : null;
        String explanation = actualQMeta[4] != null ? actualQMeta[4].toString() : null;
        Integer orderIndex = actualQMeta[5] != null ? ((Number) actualQMeta[5]).intValue() : null;
        
        // Get options metadata using native query
        var optsMeta = optionRepo.findOptionMetadataByQuestionId(questionId);
        List<OptionDto> options = optsMeta.stream()
                .map(optMetaArray -> {
                    Object[] actualOptMetaArray = optMetaArray;
                    if (optMetaArray.length == 1 && optMetaArray[0] instanceof Object[]) {
                        actualOptMetaArray = (Object[]) optMetaArray[0];
                    }
                    // Option metadata: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
                    Long optId = ((Number) actualOptMetaArray[0]).longValue();
                    String optContent = actualOptMetaArray[2] != null ? actualOptMetaArray[2].toString() : null;
                    Boolean isCorrect = actualOptMetaArray[3] != null ? 
                        (actualOptMetaArray[3] instanceof Boolean ? (Boolean) actualOptMetaArray[3] :
                         ((Number) actualOptMetaArray[3]).intValue() != 0) : false;
                    Integer optOrderIndex = actualOptMetaArray[4] != null ? ((Number) actualOptMetaArray[4]).intValue() : null;
                    return new OptionDto(optId, optContent, isCorrect, optOrderIndex);
                })
                .toList();
        
        return new QuestionWithOptionsDto(qId, content, explanation, questionType, orderIndex, options);
    }

    public void deleteQuestion(Long lessonId, Long questionId){
        Question qu = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        Long lessonIdFromDb = qu.getQuiz().getLesson().getId();
        requireOwner(lessonIdFromDb);

        Quiz quiz = qu.getQuiz();
        optionRepo.findByQuestion_IdOrderByOrderIndexAsc(questionId).forEach(optionRepo::delete);
        questionRepo.delete(qu);
        refreshTotalQuestions(quiz);
    }

    /* ---------- Options ---------- */

    public List<OptionDto> addOptions(Long lessonId, Long questionId, List<OptionUpsertReq> reqs){
        Question qu = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        Long lessonIdFromDb = qu.getQuiz().getLesson().getId();
        requireOwner(lessonIdFromDb);

        if (reqs == null || reqs.isEmpty()) throw new RuntimeException("Options are required");
        long trueCount = reqs.stream().filter(r -> Boolean.TRUE.equals(r.isCorrect())).count();
        if (trueCount != 1) throw new RuntimeException("Exactly ONE option must be correct");

        for (OptionUpsertReq r : reqs){
            Option o = new Option();
            o.setQuestion(qu);
            o.setContent(r.content());
            o.setIsCorrect(Boolean.TRUE.equals(r.isCorrect()));
            o.setOrderIndex(r.orderIndex() == null ? 0 : r.orderIndex());
            optionRepo.save(o);
        }
        
        // Use native query to avoid LOB stream error when returning response
        var optsMeta = optionRepo.findOptionMetadataByQuestionId(questionId);
        return optsMeta.stream()
                .map(optMetaArray -> {
                    Object[] actualOptMetaArray = optMetaArray;
                    if (optMetaArray.length == 1 && optMetaArray[0] instanceof Object[]) {
                        actualOptMetaArray = (Object[]) optMetaArray[0];
                    }
                    // Option metadata: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
                    Long optId = ((Number) actualOptMetaArray[0]).longValue();
                    String optContent = actualOptMetaArray[2] != null ? actualOptMetaArray[2].toString() : null;
                    Boolean isCorrect = actualOptMetaArray[3] != null ? 
                        (actualOptMetaArray[3] instanceof Boolean ? (Boolean) actualOptMetaArray[3] :
                         ((Number) actualOptMetaArray[3]).intValue() != 0) : false;
                    Integer optOrderIndex = actualOptMetaArray[4] != null ? ((Number) actualOptMetaArray[4]).intValue() : null;
                    return new OptionDto(optId, optContent, isCorrect, optOrderIndex);
                })
                .toList();
    }

    public OptionDto updateOption(Long lessonId, Long optionId, OptionUpsertReq req){
        Option o = optionRepo.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found"));
        Long lessonIdFromDb = o.getQuestion().getQuiz().getLesson().getId();
        requireOwner(lessonIdFromDb);

        if (req.content() != null) o.setContent(req.content());
        if (req.orderIndex() != null) o.setOrderIndex(req.orderIndex());

        if (req.isCorrect() != null) {
            if (Boolean.TRUE.equals(req.isCorrect())) {
                // set các option khác về false
                optionRepo.findByQuestion_IdOrderByOrderIndexAsc(o.getQuestion().getId())
                        .forEach(other -> {
                            if (!other.getId().equals(o.getId()) && Boolean.TRUE.equals(other.getIsCorrect())) {
                                other.setIsCorrect(false);
                                optionRepo.save(other);
                            }
                        });
                o.setIsCorrect(true);
            } else {
                long correctNow = optionRepo.countByQuestion_IdAndIsCorrectTrue(o.getQuestion().getId());
                if (correctNow <= 1 && Boolean.TRUE.equals(o.getIsCorrect())) {
                    throw new RuntimeException("At least ONE option must be correct");
                }
                o.setIsCorrect(false);
            }
        }

        optionRepo.save(o);
        
        // Use native query to avoid LOB stream error when returning response
        var optionMetadataOpt = optionRepo.findOptionMetadataById(optionId);
        if (optionMetadataOpt.isEmpty()) {
            throw new RuntimeException("Failed to retrieve updated option");
        }
        
        Object[] optMeta = optionMetadataOpt.get();
        Object[] actualOptMeta = optMeta;
        if (optMeta.length == 1 && optMeta[0] instanceof Object[]) {
            actualOptMeta = (Object[]) optMeta[0];
        }
        
        // Option metadata: [id, questionId, content, isCorrect, orderIndex, createdAt, updatedAt]
        Long optId = ((Number) actualOptMeta[0]).longValue();
        String optContent = actualOptMeta[2] != null ? actualOptMeta[2].toString() : null;
        Boolean isCorrect = actualOptMeta[3] != null ? 
            (actualOptMeta[3] instanceof Boolean ? (Boolean) actualOptMeta[3] :
             ((Number) actualOptMeta[3]).intValue() != 0) : false;
        Integer optOrderIndex = actualOptMeta[4] != null ? ((Number) actualOptMeta[4]).intValue() : null;
        
        return new OptionDto(optId, optContent, isCorrect, optOrderIndex);
    }

    public void deleteOption(Long lessonId, Long optionId){
        Option o = optionRepo.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found"));
        Long lessonIdFromDb = o.getQuestion().getQuiz().getLesson().getId();
        requireOwner(lessonIdFromDb);

        Long qid = o.getQuestion().getId();
        int total = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(qid).size();
        if (total <= 2) throw new RuntimeException("Question must have at least 2 options");
        if (Boolean.TRUE.equals(o.getIsCorrect()))
            throw new RuntimeException("Cannot delete the only correct option. Set another option correct first.");
        optionRepo.delete(o);
    }
}
