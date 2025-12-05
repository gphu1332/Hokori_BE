// com.hokori.web.repository.FlashcardRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findBySetAndDeletedFlagFalseOrderByOrderIndexAsc(FlashcardSet set);

    /**
     * Find flashcards by setId with eager fetching of set, set.createdBy, and set.createdBy.role
     * to avoid LazyInitializationException when serializing response.
     */
    @Query("SELECT DISTINCT c FROM Flashcard c " +
           "LEFT JOIN FETCH c.set s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE c.set.id = :setId AND c.deletedFlag = false " +
           "ORDER BY c.orderIndex ASC")
    List<Flashcard> findBySetIdWithSetAndCreatedByOrderByOrderIndexAsc(@Param("setId") Long setId);

    long countBySet_CreatedBy_IdAndSet_DeletedFlagFalseAndDeletedFlagFalse(Long userId);

    long countBySet_CreatedBy_IdAndSet_LevelAndSet_DeletedFlagFalseAndDeletedFlagFalse(
            Long userId,
            String level
    );
}
