package com.hokori.web.repository;

import com.hokori.web.entity.JlptTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptTestRepository extends JpaRepository<JlptTest, Long> {
    List<JlptTest> findByEvent_IdAndDeletedFlagFalse(Long eventId);

    // Lấy tất cả đề chưa bị xoá, sort theo level + createdAt desc
    List<JlptTest> findByDeletedFlagFalseOrderByLevelAscCreatedAtDesc();
}
