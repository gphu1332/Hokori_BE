// com.hokori.web.repository.FlashcardRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findBySetAndDeletedFlagFalseOrderByOrderIndexAsc(FlashcardSet set);
}
