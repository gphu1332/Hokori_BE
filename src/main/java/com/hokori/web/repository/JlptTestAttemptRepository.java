package com.hokori.web.repository;

import com.hokori.web.entity.JlptTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptTestAttemptRepository extends JpaRepository<JlptTestAttempt, Long> {
    
    // Lấy tất cả attempts của 1 user cho 1 test (sắp xếp theo thời gian nộp bài mới nhất)
    List<JlptTestAttempt> findByUser_IdAndTest_IdOrderBySubmittedAtDesc(Long userId, Long testId);
    
    // Lấy attempt mới nhất của user cho 1 test
    JlptTestAttempt findFirstByUser_IdAndTest_IdOrderBySubmittedAtDesc(Long userId, Long testId);
    
    // Đếm số lần user đã làm test này
    Long countByUser_IdAndTest_Id(Long userId, Long testId);
}

