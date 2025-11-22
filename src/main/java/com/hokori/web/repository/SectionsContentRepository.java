// com/hokori/web/repository/SectionsContentRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.SectionsContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionsContentRepository extends JpaRepository<SectionsContent, Long> {
    long countBySection_Id(Long sectionId);

    List<SectionsContent> findBySection_IdOrderByOrderIndexAsc(Long sectionId);
    
    /**
     * Get courseId from sectionContentId (for enrollment check)
     */
    @Query("""
        SELECT sc.section.lesson.chapter.course.id 
        FROM SectionsContent sc 
        WHERE sc.id = :sectionContentId
        """)
    Optional<Long> findCourseIdBySectionContentId(@Param("sectionContentId") Long sectionContentId);
    
    /**
     * Get course owner userId from sectionContentId (for teacher validation)
     */
    @Query("""
        SELECT sc.section.lesson.chapter.course.userId 
        FROM SectionsContent sc 
        WHERE sc.id = :sectionContentId
        """)
    Optional<Long> findCourseOwnerIdBySectionContentId(@Param("sectionContentId") Long sectionContentId);
}
