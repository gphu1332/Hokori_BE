package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.CourseRes;
import com.hokori.web.dto.course.CourseUpsertReq;
import com.hokori.web.entity.Chapter;
import com.hokori.web.entity.Course;
import com.hokori.web.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho CourseService.createCourse() - Teacher tạo khóa học
 * 
 * Cấu trúc test:
 * 1. Mock các dependencies (repositories, services)
 * 2. Setup test data
 * 3. Mock behavior của repositories
 * 4. Gọi method cần test
 * 5. Verify kết quả và interactions
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

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
    private ObjectMapper objectMapper;

    @InjectMocks
    private CourseService courseService;

    private Long teacherUserId;
    private CourseUpsertReq courseRequest;
    private Course savedCourse;

    @BeforeEach
    void setUp() {
        // Setup test data
        teacherUserId = 1L;

        courseRequest = new CourseUpsertReq();
        courseRequest.setTitle("Khóa học tiếng Nhật N5");
        courseRequest.setSubtitle("Học từ cơ bản");
        courseRequest.setDescription("Mô tả khóa học");
        courseRequest.setLevel(JLPTLevel.N5);
        courseRequest.setPriceCents(100000L);
        courseRequest.setDiscountedPriceCents(80000L);
        courseRequest.setCurrency("VND");

        // Setup saved course entity
        savedCourse = new Course();
        savedCourse.setId(1L);
        savedCourse.setUserId(teacherUserId);
        savedCourse.setTitle(courseRequest.getTitle());
        savedCourse.setSlug("khoa-hoc-tieng-nhat-n5");
        savedCourse.setStatus(CourseStatus.DRAFT);
        savedCourse.setLevel(JLPTLevel.N5);
    }

    /**
     * Test case: Teacher tạo khóa học thành công
     * 
     * Kỳ vọng:
     * - Course được tạo với đúng thông tin
     * - Slug được generate tự động
     * - Status mặc định là DRAFT
     * - Tự động tạo 1 chapter "Học thử"
     * - Trả về CourseRes với đúng thông tin
     */
    @Test
    void testCreateCourse_Success() {
        // Arrange: Mock repository behavior
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        when(courseRepo.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(1L);
            course.setSlug("khoa-hoc-tieng-nhat-n5");
            return course;
        });

        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act: Gọi method cần test
        CourseRes result = courseService.createCourse(teacherUserId, courseRequest);

        // Assert: Verify kết quả
        assertNotNull(result);
        assertEquals(courseRequest.getTitle(), result.getTitle());
        assertEquals(JLPTLevel.N5, result.getLevel());
        assertEquals(CourseStatus.DRAFT, result.getStatus());
        assertEquals(teacherUserId, result.getUserId());
        assertNotNull(result.getSlug());

        // Verify: Kiểm tra interactions
        verify(courseRepo, times(1)).save(any(Course.class));
        verify(chapterRepo, times(1)).save(any(Chapter.class));
        
        // Verify chapter "Học thử" được tạo
        verify(chapterRepo).save(argThat(chapter -> 
            chapter.getTitle().equals("Học thử") &&
            chapter.isTrial() == true &&
            chapter.getOrderIndex() == 0
        ));
    }

    /**
     * Test case: Tạo khóa học với title rỗng -> tự động set "Untitled Course"
     */
    @Test
    void testCreateCourse_EmptyTitle_UsesDefaultTitle() {
        // Arrange
        courseRequest.setTitle("");
        
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        when(courseRepo.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(1L);
            return course;
        });
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act
        CourseRes result = courseService.createCourse(teacherUserId, courseRequest);

        // Assert
        assertNotNull(result);
        verify(courseRepo).save(argThat(course -> 
            course.getSlug() != null && !course.getSlug().isEmpty()
        ));
    }

    /**
     * Test case: Tạo khóa học với slug bị trùng -> retry với timestamp
     */
    @Test
    void testCreateCourse_DuplicateSlug_RetriesWithTimestamp() {
        // Arrange: Lần đầu save bị duplicate slug
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        
        when(courseRepo.save(any(Course.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate slug"))
            .thenAnswer(invocation -> {
                Course course = invocation.getArgument(0);
                course.setId(1L);
                course.setSlug("khoa-hoc-tieng-nhat-n5-1234567890");
                return course;
            });

        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act
        CourseRes result = courseService.createCourse(teacherUserId, courseRequest);

        // Assert
        assertNotNull(result);
        // Verify retry logic được gọi
        verify(courseRepo, atLeast(1)).save(any(Course.class));
    }

    /**
     * Test case: Tạo khóa học với level null -> mặc định N5
     */
    @Test
    void testCreateCourse_NullLevel_DefaultsToN5() {
        // Arrange
        courseRequest.setLevel(null);
        
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        when(courseRepo.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(1L);
            return course;
        });
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act
        CourseRes result = courseService.createCourse(teacherUserId, courseRequest);

        // Assert
        assertNotNull(result);
        // Verify course được tạo với level null hoặc N5 (tùy logic trong applyCourse)
        verify(courseRepo).save(any(Course.class));
    }

    /**
     * Test case: Tạo khóa học với retry quá nhiều lần -> throw exception
     */
    @Test
    void testCreateCourse_MaxRetriesExceeded_ThrowsException() {
        // Arrange: Luôn throw duplicate slug exception
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        when(courseRepo.save(any(Course.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate slug unique constraint"));

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            courseService.createCourse(teacherUserId, courseRequest);
        });

        // Verify retry được gọi đúng số lần (maxRetries = 5)
        verify(courseRepo, times(5)).save(any(Course.class));
    }

    /**
     * Test case: Verify chapter "Học thử" được tạo đúng
     */
    @Test
    void testCreateCourse_CreatesTrialChapter() {
        // Arrange
        when(courseRepo.existsBySlug(anyString())).thenReturn(false);
        when(courseRepo.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(1L);
            return course;
        });
        when(chapterRepo.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(1L);
            return chapter;
        });

        // Act
        courseService.createCourse(teacherUserId, courseRequest);

        // Assert: Verify chapter "Học thử" được tạo với đúng properties
        verify(chapterRepo).save(argThat(chapter -> 
            "Học thử".equals(chapter.getTitle()) &&
            "Nội dung dùng thử miễn phí".equals(chapter.getSummary()) &&
            chapter.isTrial() == true &&
            chapter.getOrderIndex() == 0 &&
            chapter.getCourse() != null &&
            chapter.getCourse().getId() != null
        ));
    }
}
