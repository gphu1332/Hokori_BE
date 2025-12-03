// com.hokori.web.service.FlashcardStudyService.java
package com.hokori.web.service;

import com.hokori.web.Enum.FlashcardProgressStatus;
import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.User;
import com.hokori.web.entity.UserFlashcardProgress;
import com.hokori.web.repository.FlashcardRepository;
import com.hokori.web.repository.UserFlashcardProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import com.hokori.web.dto.flashcard.UserFlashcardProgressResponse;

@Service
@RequiredArgsConstructor
public class FlashcardStudyService {

    private final FlashcardRepository cardRepo;
    private final UserFlashcardProgressRepository progressRepo;
    private final LearnerProgressService learnerProgressService;

    @Transactional(readOnly = true)
    public UserFlashcardProgressResponse getProgress(Long userId, Long flashcardId) {
        // Verify flashcard exists
        if (!cardRepo.existsById(flashcardId)) {
            throw new EntityNotFoundException("Flashcard not found");
        }

        // Get progress if exists
        Optional<UserFlashcardProgress> progressOpt = progressRepo.findByUser_IdAndFlashcard_Id(userId, flashcardId);

        if (progressOpt.isPresent()) {
            return UserFlashcardProgressResponse.fromEntity(progressOpt.get());
        }

        // Return null or default response if no progress exists
        // FE can check if response is null or has default values
        return null;
    }

    @Transactional
    public UserFlashcardProgress updateProgress(User user, Long flashcardId,
                                                FlashcardProgressStatus status) {

        Flashcard card = cardRepo.findById(flashcardId)
                .orElseThrow(() -> new EntityNotFoundException("Flashcard not found"));

        UserFlashcardProgress progress = progressRepo.findByUserAndFlashcard(user, card)
                .orElseGet(() -> UserFlashcardProgress.builder()
                        .user(user)
                        .flashcard(card)
                        .build());

        progress.setStatus(status);
        progress.setLastReviewedAt(Instant.now());
        progress.setReviewCount(progress.getReviewCount() + 1);

        if (status == FlashcardProgressStatus.MASTERED && progress.getMasteredAt() == null) {
            progress.setMasteredAt(Instant.now());
        }
        progress = progressRepo.save(progress);
        learnerProgressService.recordLearningActivity(user.getId(), Instant.now());

        return progress;
    }
}
