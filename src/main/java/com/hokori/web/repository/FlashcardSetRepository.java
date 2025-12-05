// com.hokori.web.repository.FlashcardSetRepository.java
package com.hokori.web.repository;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.entity.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long> {

    List<FlashcardSet> findByCreatedBy_IdAndDeletedFlagFalse(Long userId);

    List<FlashcardSet> findByCreatedBy_IdAndTypeAndDeletedFlagFalse(Long userId, FlashcardSetType type);

    Optional<FlashcardSet> findBySectionContent_IdAndDeletedFlagFalse(Long sectionContentId);

    /**
     * Find flashcard set by section content ID with eager fetching of createdBy and role
     * to avoid LazyInitializationException when serializing response.
     */
    @Query("SELECT s FROM FlashcardSet s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE s.sectionContent.id = :sectionContentId AND s.deletedFlag = false")
    Optional<FlashcardSet> findBySectionContent_IdAndDeletedFlagFalseWithCreatedBy(@Param("sectionContentId") Long sectionContentId);

    /**
     * Find flashcard set by ID with eager fetching of createdBy, role, and sectionContent
     * to avoid LazyInitializationException when serializing response.
     */
    @Query("SELECT s FROM FlashcardSet s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH u.role " +
           "LEFT JOIN FETCH s.sectionContent " +
           "WHERE s.id = :id AND s.deletedFlag = false")
    Optional<FlashcardSet> findByIdWithCreatedBy(@Param("id") Long id);

    /**
     * Find flashcard sets by createdBy with eager fetching of createdBy and role
     * to avoid LazyInitializationException when serializing response.
     */
    @Query("SELECT DISTINCT s FROM FlashcardSet s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE s.createdBy.id = :userId AND s.deletedFlag = false")
    List<FlashcardSet> findByCreatedBy_IdAndDeletedFlagFalseWithCreatedBy(@Param("userId") Long userId);

    /**
     * Find flashcard sets by createdBy and type with eager fetching of createdBy and role
     * to avoid LazyInitializationException when serializing response.
     */
    @Query("SELECT DISTINCT s FROM FlashcardSet s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE s.createdBy.id = :userId AND s.type = :type AND s.deletedFlag = false")
    List<FlashcardSet> findByCreatedBy_IdAndTypeAndDeletedFlagFalseWithCreatedBy(
            @Param("userId") Long userId, 
            @Param("type") FlashcardSetType type);

    long countByCreatedBy_IdAndDeletedFlagFalse(Long userId);

    long countByCreatedBy_IdAndLevelAndDeletedFlagFalse(Long userId, String level);
}
