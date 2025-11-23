// com.hokori.web.service.FlashcardSetService.java
package com.hokori.web.service;

import com.hokori.web.Enum.FlashcardProgressStatus;
import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.dto.flashcard.FlashcardDashboardResponse;
import com.hokori.web.entity.*;
import com.hokori.web.repository.FlashcardRepository;
import com.hokori.web.repository.FlashcardSetRepository;
import com.hokori.web.repository.UserFlashcardProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardSetService {

    private final FlashcardSetRepository setRepo;
    private final FlashcardRepository cardRepo;
    private final UserFlashcardProgressRepository progressRepo;

    // =======================
    // CREATE SET
    // =======================

    @Transactional
    public FlashcardSet createPersonalSet(User owner, String title, String description, String level) {
        FlashcardSet set = FlashcardSet.builder()
                .createdBy(owner)
                .title(title)
                .description(description)
                .level(level)
                .type(FlashcardSetType.PERSONAL)
                .build();
        return setRepo.save(set);
    }

    @Transactional
    public FlashcardSet createCourseVocabSet(User teacher,
                                             SectionsContent sectionContent,
                                             String title,
                                             String description,
                                             String level) {
        FlashcardSet set = FlashcardSet.builder()
                .createdBy(teacher)
                .title(title)
                .description(description)
                .level(level)
                .type(FlashcardSetType.COURSE_VOCAB)
                .sectionContent(sectionContent)
                .build();
        return setRepo.save(set);
    }

    public FlashcardSet getSetOrThrow(Long id) {
        return setRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlashcardSet not found"));
    }

    // =======================
    // CARD CRUD
    // =======================

    @Transactional
    public Flashcard addCardToSet(Long setId,
                                  String front,
                                  String back,
                                  String reading,
                                  String example,
                                  Integer orderIndex) {
        FlashcardSet set = getSetOrThrow(setId);
        Flashcard card = Flashcard.builder()
                .set(set)
                .frontText(front)
                .backText(back)
                .reading(reading)
                .exampleSentence(example)
                .orderIndex(orderIndex)
                .build();
        return cardRepo.save(card);
    }

    public List<Flashcard> listCards(Long setId) {
        FlashcardSet set = getSetOrThrow(setId);
        return cardRepo.findBySetAndDeletedFlagFalseOrderByOrderIndexAsc(set);
    }

    public FlashcardSet updateSet(Long setId,
                                  String title,
                                  String description,
                                  String level) {
        FlashcardSet set = getSetOrThrow(setId);
        set.setTitle(title);
        set.setDescription(description);
        set.setLevel(level);
        return set; // entity managed, auto flush
    }

    public Flashcard updateCardInSet(Long setId,
                                     Long cardId,
                                     String frontText,
                                     String backText,
                                     String reading,
                                     String exampleSentence,
                                     Integer orderIndex) {

        Flashcard card = cardRepo.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Flashcard not found"));

        // Đảm bảo card thuộc đúng set
        if (!card.getSet().getId().equals(setId)) {
            throw new IllegalArgumentException("Flashcard does not belong to this set");
        }

        card.setFrontText(frontText);
        card.setBackText(backText);
        card.setReading(reading);
        card.setExampleSentence(exampleSentence);
        card.setOrderIndex(orderIndex);

        return card;
    }

    @Transactional
    public void deleteCardFromSet(Long setId, Long cardId) {
        Flashcard card = cardRepo.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Flashcard not found"));

        // Đảm bảo card thuộc đúng set
        if (!card.getSet().getId().equals(setId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Flashcard does not belong to this set"
            );
        }

        // Soft delete để tránh lỗi FK (user_flashcard_progress đang tham chiếu)
        if (!card.isDeletedFlag()) {
            card.setDeletedFlag(true);
            cardRepo.save(card);
        }
    }

    @Transactional
    public void softDeleteSet(Long setId) {
        FlashcardSet set = getSetOrThrow(setId);

        if (set.isDeletedFlag()) {
            return;
        }

        // 1. Soft delete set
        set.setDeletedFlag(true);

        // 2. Soft delete các card thuộc set
        if (set.getCards() != null) {
            for (Flashcard card : set.getCards()) {
                card.setDeletedFlag(true);
            }
        }
    }

    // =======================
    // DASHBOARD
    // =======================

    public FlashcardDashboardResponse getDashboard(Long userId, String level) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        // 1) Đếm số set user tạo
        long totalSets = (level == null || level.isBlank())
                ? setRepo.countByCreatedBy_IdAndDeletedFlagFalse(userId)
                : setRepo.countByCreatedBy_IdAndLevelAndDeletedFlagFalse(userId, level);

        // 2) Đếm số card trong các set đó
        long totalCards = (level == null || level.isBlank())
                ? cardRepo.countBySet_CreatedBy_IdAndSet_DeletedFlagFalseAndDeletedFlagFalse(userId)
                : cardRepo.countBySet_CreatedBy_IdAndSet_LevelAndSet_DeletedFlagFalseAndDeletedFlagFalse(userId, level);

        // 3) Lấy toàn bộ progress của user (filter theo level nếu có)
        List<UserFlashcardProgress> progresses = (level == null || level.isBlank())
                ? progressRepo.findByUser_Id(userId)
                : progressRepo.findByUser_IdAndFlashcard_Set_Level(userId, level);

        // 3.1) Đã ôn hôm nay
        long reviewedToday = progresses.stream()
                .filter(p -> p.getLastReviewedAt() != null)
                .map(p -> LocalDateTime.ofInstant(p.getLastReviewedAt(), zone).toLocalDate())
                .filter(d -> d.equals(today))
                .count();

        // 3.2) Chuỗi ngày học
        int streakDays = calculateStreakDaysFromProgresses(progresses, today, zone);

        return new FlashcardDashboardResponse(
                totalSets,
                totalCards,
                reviewedToday,
                streakDays
        );
    }

    private int calculateStreakDaysFromProgresses(
            List<UserFlashcardProgress> progresses,
            LocalDate today,
            ZoneId zone
    ) {
        if (progresses.isEmpty()) return 0;

        // Tập các ngày user có ôn thẻ
        Set<LocalDate> days = progresses.stream()
                .filter(p -> p.getLastReviewedAt() != null)
                .map(p -> LocalDateTime.ofInstant(p.getLastReviewedAt(), zone).toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate d = today;
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    // =======================
    // REVIEW CARD
    // =======================

    public UserFlashcardProgress markCardReviewed(User user, Flashcard card, boolean mastered) {
        UserFlashcardProgress p = progressRepo
                .findByUser_IdAndFlashcard_Id(user.getId(), card.getId())
                .orElseGet(() -> UserFlashcardProgress.builder()
                        .user(user)
                        .flashcard(card)
                        .build());

        p.setReviewCount(p.getReviewCount() + 1);
        Instant now = Instant.now();
        p.setLastReviewedAt(now);

        if (mastered) {
            p.setStatus(FlashcardProgressStatus.MASTERED);
            if (p.getMasteredAt() == null) {
                p.setMasteredAt(now);
            }
        }

        return progressRepo.save(p);
    }
}
