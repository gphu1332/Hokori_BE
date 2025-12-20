package com.hokori.web.service;

import com.hokori.web.dto.quiz.OptionUpsertReq;
import com.hokori.web.dto.quiz.QuestionUpsertReq;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherQuizServiceTest {

    @Mock CurrentUserService current;
    @Mock LessonRepository lessonRepo;
    @Mock SectionRepository sectionRepo;
    @Mock QuizRepository quizRepo;
    @Mock QuestionRepository questionRepo;
    @Mock OptionRepository optionRepo;
    @Mock SectionsContentRepository contentRepo;
    @Mock CourseService courseService;

    @InjectMocks
    private TeacherQuizService service;

    /* ===================== SECURITY ===================== */

    @Test
    void requireOwner_notOwner_forbidden() {
        User me = new User();
        me.setId(1L);

        when(current.getCurrentUserOrThrow()).thenReturn(me);
        when(lessonRepo.findCourseOwnerIdByLessonId(10L)).thenReturn(Optional.of(99L));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.getQuizBySection(10L)
        );

        assertTrue(ex.getMessage().contains("not the owner"));
    }

    /* ===================== QUIZ ===================== */

    @Test
    void createQuiz_whenQuizAlreadyExists_throwException() {
        Section section = new Section();
        section.setId(1L);

        when(sectionRepo.findById(1L)).thenReturn(Optional.of(section));
        when(quizRepo.findQuizMetadataBySectionId(1L))
                .thenReturn(Optional.of(new Object[10]));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.createQuiz(1L, mockQuizReq())
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }

    /* ===================== QUESTION ===================== */

    @Test
    void createQuestion_increaseTotalQuestions() {
        Quiz quiz = new Quiz();
        quiz.setId(1L);

        Section section = new Section();
        section.setId(2L);
        quiz.setSection(section);

        when(questionRepo.countByQuiz_Id(1L)).thenReturn(0L);
        when(quizRepo.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        QuestionUpsertReq req = new QuestionUpsertReq(
                "Q1", "Explain", "SINGLE_CHOICE", null
        );

        service.createQuestion(2L, 1L, req);

        verify(questionRepo).save(any());
        verify(quizRepo).save(any());
    }

    /* ===================== OPTION ===================== */

    @Test
    void addOptions_moreThanOneCorrect_throwException() {
        OptionUpsertReq o1 = new OptionUpsertReq("A", true, 1);
        OptionUpsertReq o2 = new OptionUpsertReq("B", true, 2);

        Question q = new Question();
        Quiz quiz = new Quiz();
        Section section = new Section();
        section.setId(1L);
        quiz.setSection(section);
        q.setQuiz(quiz);

        when(questionRepo.findById(1L)).thenReturn(Optional.of(q));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.addOptions(1L, 1L, List.of(o1, o2))
        );

        assertTrue(ex.getMessage().contains("ONE option"));
    }

    @Test
    void deleteOption_onlyCorrectOption_throwException() {
        Option o = new Option();
        o.setId(1L);
        o.setIsCorrect(true);

        Question q = new Question();
        q.setId(2L);
        Quiz quiz = new Quiz();
        Section section = new Section();
        section.setId(3L);
        quiz.setSection(section);
        q.setQuiz(quiz);
        o.setQuestion(q);

        when(optionRepo.findById(1L)).thenReturn(Optional.of(o));
        when(optionRepo.findByQuestion_IdOrderByOrderIndexAsc(2L))
                .thenReturn(List.of(o, new Option()));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.deleteOption(3L, 1L)
        );

        assertTrue(ex.getMessage().contains("correct option"));
    }

    /* ===================== HELPERS ===================== */

    private com.hokori.web.dto.quiz.QuizUpsertReq mockQuizReq() {
        return new com.hokori.web.dto.quiz.QuizUpsertReq(
                "Quiz", "Desc", 300, 70
        );
    }
}
