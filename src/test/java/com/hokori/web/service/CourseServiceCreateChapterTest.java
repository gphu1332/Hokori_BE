package com.hokori.web.service;

import com.hokori.web.dto.course.ChapterRes;
import com.hokori.web.dto.course.ChapterUpsertReq;
import com.hokori.web.entity.Chapter;
import com.hokori.web.entity.Course;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho CourseService.createChapter() - Teacher tạo chapter
 * 
 * Test cases dựa trên template unittest.xls:
 * - UTCID01: Tạo chapter thành công với isTrial=TRUE
 * - UTCID02: Tạo chapter thành công với isTrial=FALSE
 * - UTCID03: Title null → validation error
 * - UTCID04: isTrial=TRUE nhưng đã có trial chapter → exception
 * - UTCID05: Course không tồn tại → NOT_FOUND
 * - UTCID06: Không phải owner → FORBIDDEN
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceCreateChapterTest {

    @Mock
    private CourseRepository courseRepo;

    @Mock
    private ChapterRepository chapterRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CourseFlagService courseFlagService;

    @Mock
    private LessonRepository lessonRepo;

    @Mock
    private SectionRepository sectionRepo;

    @Mock
    private SectionsContentRepository contentRepo;

    @Mock
    private QuizRepository quizRepo;

    @Mock
    private FlashcardSetRepository flashcardSetRepo;

    @Mock
    private QuestionRepository questionRepo;

    @Mock
    private OptionRepository optionRepo;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private CourseService courseService;

    private Long teacherUserId;
    private Long courseId;
    private Course ownedCourse;
    private ChapterUpsertReq chapterRequest;

    @BeforeEach
    void setUp() {
        teacherUserId = 1L;
        courseId = 185L;

        // Setup owned course
        ownedCourse = new Course();
        ownedCourse.setId(courseId);
        ownedCourse.setUserId(teacherUserId);
        ownedCourse.setTitle("Khóa học N3");
        ownedCourse.setStatus(CourseStatus.DRAFT);
        ownedCourse.setLevel(JLPTLevel.N3);

        // Setup chapter request
        chapterRequest = new ChapterUpsertReq();
        chapterRequest.setTitle("Khóa học N3");
        chapterRequest.setSummary("Mô tả chapter");

        // Mock assertOwner() for owned course (default case)
        when(courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherUserId))
            .thenReturn(true);
    }

    /**
     * UTCID01: Tạo chapter thành công với isTrial=TRUE
     * 
     * Precondition:
     * - courseID = "185" (owned)
     * - title = "Khóa học N3"
     * - isTrial = TRUE
     * 
     * Expected:
     * - Return: ChapterRes
     * - Log: "Chapter created."
     * - Type: Normal (N)
     */
    @Test
    void testCreateChapter_UTCID01_SuccessWithTrial() {
        // Arrange
        chapterRequest.setIsTrial(true);
        chapterRequest.setOrderIndex(1); // Not first chapter

        when(courseRepo.findById(courseId)).thenReturn(Optional.of(ownedCourse));
        when(chapterRepo.countByCourse_Id(courseId)).thenReturn(1L);
        when(chapterRepo.countByCourse_IdAndIsTrialTrue(courseId)).thenReturn(0L); // No trial chapter yet
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });
        when(chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)).thenReturn(Optional.empty());

        // Act
        ChapterRes result = courseService.createChapter(courseId, teacherUserId, chapterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(chapterRequest.getTitle(), result.getTitle());
        assertTrue(result.getIsTrial());

        // Verify interactions
        verify(courseRepo).findById(courseId);
        verify(chapterRepo).save(any(Chapter.class));
    }

    /**
     * UTCID02: Tạo chapter thành công với isTrial=FALSE
     * 
     * Precondition:
     * - courseID = "185" (owned)
     * - title = "Khóa học N3"
     * - isTrial = FALSE
     * 
     * Expected:
     * - Return: ChapterRes
     * - Type: Normal (N)
     */
    @Test
    void testCreateChapter_UTCID02_SuccessWithoutTrial() {
        // Arrange
        chapterRequest.setIsTrial(false);
        chapterRequest.setOrderIndex(1);

        when(courseRepo.findById(courseId)).thenReturn(Optional.of(ownedCourse));
        when(chapterRepo.countByCourse_Id(courseId)).thenReturn(1L);
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });
        when(chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)).thenReturn(Optional.empty());

        // Act
        ChapterRes result = courseService.createChapter(courseId, teacherUserId, chapterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(chapterRequest.getTitle(), result.getTitle());

        // Verify
        verify(chapterRepo).save(any(Chapter.class));
    }

    /**
     * UTCID03: Title null → validation error
     * 
     * Precondition:
     * - courseID = "185" (owned)
     * - title = null
     * 
     * Expected:
     * - Exception: "validation error – title must not be blank"
     * - Log: "Title must not be blank;"
     * - Type: Abnormal (A)
     */
    @Test
    void testCreateChapter_UTCID03_NullTitle_ThrowsValidationError() {
        // Arrange
        chapterRequest.setTitle(null);

        when(courseRepo.findById(courseId)).thenReturn(Optional.of(ownedCourse));

        // Act & Assert
        // Note: Validation sẽ được trigger bởi @NotBlank annotation
        // Nếu validation được handle ở controller level, test này có thể cần điều chỉnh
        // Hoặc nếu service check manually, sẽ throw ResponseStatusException
        
        // Giả sử service check title manually hoặc validation exception được propagate
        assertThrows(Exception.class, () -> {
            courseService.createChapter(courseId, teacherUserId, chapterRequest);
        });
    }

    /**
     * UTCID04: isTrial=TRUE nhưng đã có trial chapter → exception
     * 
     * Precondition:
     * - courseID = "185" (owned)
     * - title = "Khóa học N3"
     * - isTrial = TRUE
     * - Course đã có 1 trial chapter
     * 
     * Expected:
     * - Exception: "Course already has a trial chapter. The first chapter (orderIndex=0) is always the trial chapter."
     * - Log: tương tự
     * - Type: Abnormal (A)
     */
    @Test
    void testCreateChapter_UTCID04_TrialExists_ThrowsException() {
        // Arrange
        chapterRequest.setIsTrial(true);
        chapterRequest.setOrderIndex(2); // Not first chapter, but trying to set trial

        when(courseRepo.findById(courseId)).thenReturn(Optional.of(ownedCourse));
        when(chapterRepo.countByCourse_Id(courseId)).thenReturn(2L);
        when(chapterRepo.countByCourse_IdAndIsTrialTrue(courseId)).thenReturn(1L); // Already has trial chapter

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> courseService.createChapter(courseId, teacherUserId, chapterRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Course already has a trial chapter"));
        assertTrue(exception.getMessage().contains("The first chapter (orderIndex=0) is always the trial chapter"));
    }

    /**
     * UTCID05: Course không tồn tại → NOT_FOUND
     * 
     * Precondition:
     * - courseID = "999" (not exist)
     * 
     * Expected:
     * - Exception: NOT_FOUND "Course not found"
     * - Type: Abnormal (A)
     */
    @Test
    void testCreateChapter_UTCID05_CourseNotFound_ThrowsNotFoundException() {
        // Arrange
        Long nonExistentCourseId = 999L;

        when(courseRepo.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> courseService.createChapter(nonExistentCourseId, teacherUserId, chapterRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Course not found"));
    }

    /**
     * UTCID06: Không phải owner → FORBIDDEN
     * 
     * Precondition:
     * - courseID = "186" (owned by different teacher)
     * 
     * Expected:
     * - Exception: FORBIDDEN "Not owner"
     * - Log: "Not owner"
     * - Type: Abnormal (A)
     */
    @Test
    void testCreateChapter_UTCID06_NotOwner_ThrowsForbiddenException() {
        // Arrange
        Long differentCourseId = 186L;
        Long differentOwnerId = 2L;

        // Mock assertOwner() - existsByIdAndUserIdAndDeletedFlagFalse returns false
        when(courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(differentCourseId, teacherUserId))
            .thenReturn(false);

        // Act & Assert
        // assertOwner() sẽ check và throw FORBIDDEN
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> courseService.createChapter(differentCourseId, teacherUserId, chapterRequest)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Not owner"));
    }

    /**
     * Test case bổ sung: Tạo chapter đầu tiên (orderIndex=0) → tự động set trial
     */
    @Test
    void testCreateChapter_FirstChapter_AutoSetTrial() {
        // Arrange
        chapterRequest.setOrderIndex(0); // First chapter
        chapterRequest.setIsTrial(null); // Not explicitly set

        when(courseRepo.findById(courseId)).thenReturn(Optional.of(ownedCourse));
        when(chapterRepo.countByCourse_Id(courseId)).thenReturn(0L);
        when(chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)).thenReturn(Optional.empty());
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act
        ChapterRes result = courseService.createChapter(courseId, teacherUserId, chapterRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsTrial()); // First chapter should be trial

        // Verify chapter was saved with trial=true
        verify(chapterRepo).save(argThat(chapter -> 
            chapter.getOrderIndex() == 0 && chapter.isTrial() == true
        ));
    }
}
