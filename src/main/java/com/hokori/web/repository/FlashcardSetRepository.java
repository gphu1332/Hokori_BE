// com.hokori.web.repository.FlashcardSetRepository.java
package com.hokori.web.repository;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.entity.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long> {

    List<FlashcardSet> findByCreatedBy_IdAndDeletedFlagFalse(Long userId);

    List<FlashcardSet> findByCreatedBy_IdAndTypeAndDeletedFlagFalse(Long userId, FlashcardSetType type);

    Optional<FlashcardSet> findBySectionContent_IdAndDeletedFlagFalse(Long sectionContentId);
}
