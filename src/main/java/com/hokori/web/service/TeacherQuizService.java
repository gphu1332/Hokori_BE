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

    /* ---------- Helpers ---------- */

    private void requireOwner(Long lessonId){
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
        var quiz = quizRepo.findByLesson_Id(lessonId)
                .orElseThrow(() -> new RuntimeException("Quiz not found for this lesson"));
        return mapper.toDto(quiz);
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

        q = quizRepo.save(q);
        return mapper.toDto(q);
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

        return mapper.toDto(quizRepo.save(q));
    }

    /* ---------- Questions ---------- */

    public List<QuestionWithOptionsDto> listQuestions(Long lessonId, Long quizId){
        requireOwner(lessonId);
        var quiz = getQuizOrThrow(quizId);
        if (!quiz.getLesson().getId().equals(lessonId))
            throw new RuntimeException("Quiz doesn't belong to this lesson");

        var questions = questionRepo.findByQuiz_IdOrderByOrderIndexAsc(quizId);
        return questions.stream()
                .map(q -> mapper.toDto(q, optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId())))
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

        qu = questionRepo.save(qu);
        refreshTotalQuestions(quiz);

        return mapper.toDto(qu, List.of());
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

        qu = questionRepo.save(qu);
        var opts = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(qu.getId());
        return mapper.toDto(qu, opts);
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
        return optionRepo.findByQuestion_IdOrderByOrderIndexAsc(questionId)
                .stream().map(mapper::toDto).toList();
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

        return mapper.toDto(optionRepo.save(o));
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
