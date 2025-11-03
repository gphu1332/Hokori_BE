package com.hokori.web.repository;

import com.hokori.web.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {
    long countByLesson_Id(Long lessonId);
}
