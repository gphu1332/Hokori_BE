package com.hokori.web.repository;

import com.hokori.web.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    long countByCourse_Id(Long courseId);
    long countByCourse_IdAndIsTrialTrue(Long courseId);
    Optional<Chapter> findByCourse_IdAndIsTrialTrue(Long courseId);
}

