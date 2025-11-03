package com.hokori.web.repository;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    Optional<Course> findBySlugAndDeletedFlagFalse(String slug);

    @EntityGraph(attributePaths={
            "chapters","chapters.lessons","chapters.lessons.sections","chapters.lessons.sections.contents"
    })
    Optional<Course> findByIdAndDeletedFlagFalse(Long id);

    Page<Course> findAllByDeletedFlagFalse(Pageable pageable);
    Page<Course> findAllByUserIdAndDeletedFlagFalse(Long userId, Pageable pageable);

    @Query("select c from Course c where c.deletedFlag=false and c.status='PUBLISHED' and (:lvl is null or c.level=:lvl)")
    Page<Course> findPublishedByLevel(JLPTLevel lvl, Pageable pageable);
}
