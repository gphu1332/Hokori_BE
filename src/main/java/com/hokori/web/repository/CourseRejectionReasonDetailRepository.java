package com.hokori.web.repository;

import com.hokori.web.entity.CourseRejectionReasonDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRejectionReasonDetailRepository extends JpaRepository<CourseRejectionReasonDetail, Long> {
    List<CourseRejectionReasonDetail> findByCourse_IdOrderByCreatedAtDesc(Long courseId);
    void deleteByCourse_Id(Long courseId);
}

