package com.hokori.web.repository;

import com.hokori.web.entity.CourseRejectionReasonDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRejectionReasonDetailRepository extends JpaRepository<CourseRejectionReasonDetail, Long> {
    // Find rejection reasons for a course (not deleted)
    List<CourseRejectionReasonDetail> findByCourse_IdAndDeletedFlagFalseOrderByCreatedAtDesc(Long courseId);
    
    // Legacy method for backward compatibility (includes deleted)
    @Deprecated
    default List<CourseRejectionReasonDetail> findByCourse_IdOrderByCreatedAtDesc(Long courseId) {
        return findByCourse_IdAndDeletedFlagFalseOrderByCreatedAtDesc(courseId);
    }
    
    // Delete all rejection reasons for a course (hard delete)
    void deleteByCourse_Id(Long courseId);
}

