// com.hokori.web.repository.UserFlashcardProgressRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.User;
import com.hokori.web.entity.UserFlashcardProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFlashcardProgressRepository extends JpaRepository<UserFlashcardProgress, Long> {

    Optional<UserFlashcardProgress> findByUserAndFlashcard(User user, Flashcard flashcard);

    List<UserFlashcardProgress> findByUserAndFlashcardIn(User user, List<Flashcard> flashcards);
}
