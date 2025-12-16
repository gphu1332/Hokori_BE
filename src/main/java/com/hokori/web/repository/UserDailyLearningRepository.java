package com.hokori.web.repository;

import com.hokori.web.entity.UserDailyLearning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyLearningRepository extends JpaRepository<UserDailyLearning, Long> {

    Optional<UserDailyLearning> findByUser_IdAndLearningDate(Long userId, LocalDate learningDate);

    // Lấy danh sách ngày có học, sort DESC để tính streak
    List<UserDailyLearning> findByUser_IdOrderByLearningDateDesc(Long userId);

    List<UserDailyLearning> findByUser_IdAndLearningDateBetweenOrderByLearningDateDesc(
            Long userId, LocalDate from, LocalDate to);

    // Lấy ngày học gần nhất
    Optional<UserDailyLearning> findTopByUser_IdOrderByLearningDateDesc(Long userId);

    // Check 1 ngày cụ thể có hoạt động học không
    boolean existsByUser_IdAndLearningDate(Long userId, LocalDate learningDate);
}


