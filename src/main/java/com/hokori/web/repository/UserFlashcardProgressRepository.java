// com.hokori.web.repository.UserFlashcardProgressRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.Flashcard;
import com.hokori.web.entity.User;
import com.hokori.web.entity.UserFlashcardProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFlashcardProgressRepository extends JpaRepository<UserFlashcardProgress, Long> {

    // Dùng cho các chỗ khác trong hệ thống (nếu có)
    Optional<UserFlashcardProgress> findByUserAndFlashcard(User user, Flashcard flashcard);

    List<UserFlashcardProgress> findByUserAndFlashcardIn(User user, List<Flashcard> flashcards);

    // Dùng trong FlashcardSetService.markCardReviewed(...)
    Optional<UserFlashcardProgress> findByUser_IdAndFlashcard_Id(Long userId, Long flashcardId);

    // Dùng cho dashboard (tổng theo user)
    List<UserFlashcardProgress> findByUser_Id(Long userId);

    // Dùng cho dashboard khi filter theo JLPT level của set
    List<UserFlashcardProgress> findByUser_IdAndFlashcard_Set_Level(Long userId, String level);
}
