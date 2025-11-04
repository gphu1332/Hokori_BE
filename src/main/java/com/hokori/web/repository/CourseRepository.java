package com.hokori.web.repository;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndDeletedFlagFalse(String slug);

    // ⚠️ KHÔNG dùng @EntityGraph ở đây để tránh multiple bag fetch
    Optional<Course> findByIdAndDeletedFlagFalse(Long id);

    // (không bắt buộc cho service hiện tại, nhưng giữ cũng không sao)
    Page<Course> findAllByDeletedFlagFalse(Pageable pageable);
    Page<Course> findAllByUserIdAndDeletedFlagFalse(Long userId, Pageable pageable);

    @Query("""
       select c
       from Course c
       where c.deletedFlag = false
         and c.status = 'PUBLISHED'
         and (:lvl is null or c.level = :lvl)
    """)
    Page<Course> findPublishedByLevel(JLPTLevel lvl, Pageable pageable);

    boolean existsByIdAndUserIdAndDeletedFlagFalse(Long id, Long userId);
}
