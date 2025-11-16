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

@Service
@RequiredArgsConstructor
public class FlashcardStudyService {

    private final FlashcardRepository cardRepo;
    private final UserFlashcardProgressRepository progressRepo;

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

        return progressRepo.save(progress);
    }
}
