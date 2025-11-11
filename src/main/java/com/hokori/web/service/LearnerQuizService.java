package com.hokori.web.service;

import com.hokori.web.dto.quiz.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LearnerQuizService {

    private final QuizRepository quizRepo;
    private final LessonRepository lessonRepo;          // giữ sẵn, có thể chưa dùng
    private final EnrollmentRepository enrollRepo;      // giữ sẵn, có thể chưa dùng
    private final QuizAttemptRepository attemptRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;

    public AttemptDto startAttempt(Long lessonId, Long userId, StartAttemptReq req) {
        Quiz quiz = quizRepo.findByLesson_Id(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found for lesson " + lessonId));

        if (req == null || !Boolean.TRUE.equals(req.forceNew())) {
            var last = attemptRepo.findByUserIdAndQuiz_IdOrderByStartedAtDesc(userId, quiz.getId());
            if (!last.isEmpty() && last.get(0).getStatus() == QuizAttempt.Status.IN_PROGRESS) {
                return toDto(last.get(0), quiz.getTitle());
            }
        }

        QuizAttempt a = new QuizAttempt();
        a.setUserId(userId);
        a.setQuiz(quiz);
        a.setTotalQuestions(attemptRepo.countQuestions(quiz.getId()));
        attemptRepo.save(a);
        return toDto(a, quiz.getTitle());
    }

    @Transactional(readOnly = true)
    public PlayQuestionDto nextQuestion(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new RuntimeException("Attempt not in progress");

        var unanswered = answerRepo.findUnanswered(a.getQuiz().getId(), a.getId());
        if (unanswered.isEmpty()) return null;

        Question q = unanswered.get(0);

        var opts = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(q.getId())
                .stream()
                .map(o -> new PlayOptionDto(o.getId(), o.getContent()))
                .toList();

        // questionType có thể là String hoặc Enum → toString() an toàn
        String questionTypeStr = (q.getQuestionType() != null) ? q.getQuestionType().toString() : null;

        return new PlayQuestionDto(
                q.getId(),
                q.getContent(),
                questionTypeStr,
                q.getOrderIndex(),
                opts
        );
    }

    public void answer(Long attemptId, Long userId, Long questionId, AnswerReq req) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new RuntimeException("Attempt not in progress");

        Question q = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));
        if (!Objects.equals(q.getQuiz().getId(), a.getQuiz().getId()))
            throw new RuntimeException("Question not in this quiz");

        // DTO của bạn đang là class → dùng getOptionId()
        Option chosen = optionRepo.findById(req.optionId())   // <-- record: dùng optionId()
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));
        if (!Objects.equals(chosen.getQuestion().getId(), q.getId()))
            throw new RuntimeException("Option not in this question");

        QuizAnswer ans = answerRepo.findOne(a.getId(), q.getId()).orElse(new QuizAnswer());
        ans.setAttempt(a);
        ans.setQuestion(q);
        ans.setOption(chosen);
        answerRepo.save(ans);
    }

    public AttemptDto submit(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        if (a.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw new RuntimeException("Attempt already submitted");

        int total = (a.getTotalQuestions() != null)
                ? a.getTotalQuestions()
                : attemptRepo.countQuestions(a.getQuiz().getId());

        int correct = 0;
        var answers = answerRepo.findByAttempt_Id(a.getId());
        for (QuizAnswer ans : answers) {
            boolean ok = Boolean.TRUE.equals(ans.getOption().getIsCorrect());
            ans.setIsCorrect(ok);
            if (ok) correct++;
        }
        answerRepo.saveAll(answers);

        int score = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
        a.setCorrectCount(correct);
        a.setTotalQuestions(total);
        a.setScorePercent(score);
        a.setSubmittedAt(LocalDateTime.now());
        a.setStatus(QuizAttempt.Status.SUBMITTED);
        attemptRepo.save(a);

        return toDto(a, a.getQuiz().getTitle());
    }

    @Transactional(readOnly = true)
    public AttemptDetailDto detail(Long attemptId, Long userId) {
        QuizAttempt a = attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));

        var answers = answerRepo.findByAttempt_Id(a.getId());
        Map<Long, QuizAnswer> map = new HashMap<>();
        for (QuizAnswer x : answers) map.put(x.getQuestion().getId(), x);

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

        return new AttemptDetailDto(toDto(a, a.getQuiz().getTitle()), items);
    }

    @Transactional(readOnly = true)
    public List<AttemptDto> history(Long lessonId, Long userId) {
        Quiz quiz = quizRepo.findByLesson_Id(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));
        return attemptRepo.findByUserIdAndQuiz_IdOrderByStartedAtDesc(userId, quiz.getId())
                .stream()
                .map(x -> toDto(x, quiz.getTitle()))
                .toList();
    }

    private AttemptDto toDto(QuizAttempt a, String quizTitle) {
        return new AttemptDto(
                a.getId(),
                a.getQuiz().getId(),
                quizTitle,
                a.getStatus().name(),
                a.getTotalQuestions(),
                a.getCorrectCount(),
                a.getScorePercent(),
                a.getStartedAt(),
                a.getSubmittedAt()
        );
    }

    public record AttemptDetailDto(AttemptDto attempt, java.util.List<Item> items) {
        public record Item(Long questionId, String content, Long chosenOptionId, Long correctOptionId, Boolean isCorrect) {}
    }
}
