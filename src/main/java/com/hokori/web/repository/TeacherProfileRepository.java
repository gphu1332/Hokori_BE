package com.hokori.web.repository;

import com.hokori.web.entity.TeacherProfile;
import com.hokori.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUser(User user);
    boolean existsByUser(User user);

    @Query("SELECT p FROM TeacherProfile p JOIN FETCH p.user WHERE p.id = :id")
    Optional<TeacherProfile> findWithUser(@Param("id") Long id);
}
