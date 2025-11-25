package com.hokori.web.repository;

import com.hokori.web.entity.UserDailyLearning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyLearningRepository extends JpaRepository<UserDailyLearning, Long> {

    Optional<UserDailyLearning> findByUserIdAndLearningDate(Long userId, LocalDate learningDate);

    // Lấy danh sách ngày có học, sort DESC để tính streak
    List<UserDailyLearning> findByUserIdOrderByLearningDateDesc(Long userId);

    List<UserDailyLearning> findByUserIdAndLearningDateBetweenOrderByLearningDateDesc(
            Long userId, LocalDate from, LocalDate to);

    // Lấy ngày học gần nhất
    Optional<UserDailyLearning> findTopByUserIdOrderByLearningDateDesc(Long userId);

    // Check 1 ngày cụ thể có hoạt động học không
    boolean existsByUserIdAndLearningDate(Long userId, LocalDate learningDate);
}


