package com.hokori.web.service;

import com.hokori.web.entity.FlashcardSet;
import com.hokori.web.repository.FlashcardSetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardSetServiceTest {

    @Mock
    private FlashcardSetRepository setRepo;

    // Các dependency khác của service – KHÔNG dùng tới, chỉ cần để inject
    @Mock private com.hokori.web.repository.FlashcardRepository cardRepo;
    @Mock private com.hokori.web.repository.UserFlashcardProgressRepository progressRepo;
    @Mock private CourseService courseService;
    @Mock private com.hokori.web.repository.SectionsContentRepository contentRepo;

    @InjectMocks
    private FlashcardSetService flashcardSetService;

    /**
     * TC-FLASH-01
     * Lấy flashcard set hợp lệ theo ID
     */
    @Test
    void getSetOrThrow_validId_success() {
        // given
        Long setId = 1L;

        FlashcardSet set = new FlashcardSet();
        set.setId(setId);
        set.setTitle("JLPT N4 Vocabulary");

        when(setRepo.findById(setId))
                .thenReturn(Optional.of(set));

        // when
        FlashcardSet result = flashcardSetService.getSetOrThrow(setId);

        // then
        assertNotNull(result);
        assertEquals(setId, result.getId());
        assertEquals("JLPT N4 Vocabulary", result.getTitle());

        verify(setRepo, times(1)).findById(setId);
    }

    /**
     * TC-FLASH-02
     * Flashcard set không tồn tại
     */
    @Test
    void getSetOrThrow_notFound_throwException() {
        // given
        Long setId = 99L;

        when(setRepo.findById(setId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                EntityNotFoundException.class,
                () -> flashcardSetService.getSetOrThrow(setId)
        );

        verify(setRepo, times(1)).findById(setId);
    }
}
