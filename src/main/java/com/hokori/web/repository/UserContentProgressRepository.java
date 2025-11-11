package com.hokori.web.repository;

import com.hokori.web.entity.UserContentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserContentProgressRepository extends JpaRepository<UserContentProgress, Long> {

    Optional<UserContentProgress> findByEnrollment_IdAndContent_Id(Long enrollmentId, Long contentId);

    List<UserContentProgress> findByEnrollment_IdAndContent_IdIn(Long enrollmentId, Collection<Long> contentIds);

    @Query("""
       select count(c) from UserContentProgress c
       where c.enrollment.id = :enrollId and c.isCompleted = true
         and c.content.id in (:contentIds)
    """)
    long countCompletedInList(Long enrollId, Collection<Long> contentIds);
}
