package com.hokori.web.repository;

import com.hokori.web.entity.UserContentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserContentProgressRepository extends JpaRepository<UserContentProgress, Long> {

    Optional<UserContentProgress> findByEnrollment_IdAndContent_Id(Long enrollmentId, Long contentId);

    List<UserContentProgress> findByEnrollment_IdAndContent_IdIn(Long enrollmentId, Collection<Long> contentIds);

    /**
     * Find UserContentProgress with content eagerly fetched to avoid lazy loading issues.
     * Use this when you need to access content.getId() after the query.
     */
    @Query("""
        SELECT ucp FROM UserContentProgress ucp
        JOIN FETCH ucp.content
        WHERE ucp.enrollment.id = :enrollmentId
          AND ucp.content.id IN :contentIds
    """)
    List<UserContentProgress> findByEnrollment_IdAndContent_IdInWithContent(@Param("enrollmentId") Long enrollmentId,
                                                                              @Param("contentIds") Collection<Long> contentIds);

    @Query("""
       select count(c) from UserContentProgress c
       where c.enrollment.id = :enrollId and c.isCompleted = true
         and c.content.id in (:contentIds)
    """)
    long countCompletedInList(Long enrollId, Collection<Long> contentIds);
}
