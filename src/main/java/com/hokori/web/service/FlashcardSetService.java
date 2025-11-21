// com.hokori.web.service.FlashcardSetService.java
package com.hokori.web.service;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.FlashcardSet;
import com.hokori.web.entity.SectionsContent;
import com.hokori.web.entity.User;
import com.hokori.web.repository.FlashcardRepository;
import com.hokori.web.repository.FlashcardSetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashcardSetService {

    private final FlashcardSetRepository setRepo;
    private final FlashcardRepository cardRepo;

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

        return set;
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

        // Nếu xoá cứng:
        cardRepo.delete(card);
    }

    @Transactional
    public void softDeleteSet(Long setId) {
        FlashcardSet set = getSetOrThrow(setId);

        // Nếu đã xoá rồi thì bỏ qua
        if (set.isDeletedFlag()) {
            return;
        }

        // 1. Soft delete set
        set.setDeletedFlag(true);

        // 2. Soft delete các card thuộc set (nếu entity Flashcard có deletedFlag)
        if (set.getCards() != null) {
            for (Flashcard card : set.getCards()) {
                card.setDeletedFlag(true);
            }
        }
    }
}
