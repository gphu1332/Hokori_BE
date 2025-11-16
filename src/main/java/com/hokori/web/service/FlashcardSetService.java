// com.hokori.web.service.FlashcardSetService.java
package com.hokori.web.service;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.FlashcardSet;
import com.hokori.web.entity.SectionsContent;
import com.hokori.web.entity.User;
import com.hokori.web.repository.FlashcardRepository;
import com.hokori.web.repository.FlashcardSetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
