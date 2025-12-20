package com.hokori.web.service;

import com.hokori.web.entity.JlptTest;
import com.hokori.web.entity.JlptUserTestSession;
import com.hokori.web.entity.User;
import com.hokori.web.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JlptTestServiceTest {

    @Mock
    private JlptTestRepository testRepo;
    @Mock
    private JlptQuestionRepository questionRepo;
    @Mock
    private JlptOptionRepository optionRepo;
    @Mock
    private JlptAnswerRepository answerRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private JlptUserTestSessionRepository sessionRepo;
    @Mock
    private JlptTestAttemptRepository attemptRepo;
    @Mock
    private JlptTestAttemptAnswerRepository attemptAnswerRepo;
    @Mock
    private LearnerProgressService learnerProgressService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private JlptTestService jlptTestService;

    /**
     * TC-JLPT-START-01
     * User start JLPT test lần đầu
     */
    @Test
    void startTest_firstTime_success() {
        // given
        Long testId = 1L;
        Long userId = 10L;

        JlptTest test = JlptTest.builder().id(testId).durationMin(60).currentParticipants(0).build();

        User user = new User();
        user.setId(userId);

        when(testRepo.findById(testId)).thenReturn(Optional.of(test));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        // Lần đầu → chưa có session
        when(sessionRepo.findByTest_IdAndUser_Id(testId, userId)).thenReturn(Optional.empty());

        // upsert xong → trả về session mới
        JlptUserTestSession session = new JlptUserTestSession();
        session.setTest(test);
        session.setUser(user);
        session.setStartedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(3600));


        when(sessionRepo.findByTest_IdAndUser_Id(testId, userId)).thenReturn(Optional.of(session));

        // Không quan tâm nội dung câu hỏi → mock empty
        spyQuestionsEmpty();

        // when
        var response = jlptTestService.startTest(testId, userId);

        // then
        assertNotNull(response);

        verify(sessionRepo, atLeastOnce()).upsertSession(eq(testId), eq(userId), any(), any(), any());

        verify(answerRepo).deleteByUser_IdAndTest_Id(userId, testId);

        verify(learnerProgressService).recordLearningActivity(eq(userId), any());

        // Participant tăng
        assertEquals(1, test.getCurrentParticipants());
    }

    /**
     * Helper: mock getQuestionsWithOptions() trả list rỗng
     */
    private void spyQuestionsEmpty() {
        JlptTestService spy = Mockito.spy(jlptTestService);
        Mockito.doReturn(List.of()).when(spy).getQuestionsWithOptions(anyLong(), anyLong());
        this.jlptTestService = spy;
    }
}
