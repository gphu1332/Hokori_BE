// com.hokori.web.repository.JlptEventRepository.java
package com.hokori.web.repository;

import com.hokori.web.Enum.JlptEventStatus;
import com.hokori.web.entity.JlptEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JlptEventRepository extends JpaRepository<JlptEvent, Long> {

    // Lấy tất cả event chưa bị xóa mềm
    List<JlptEvent> findByDeletedFlagFalse();

    // Lọc theo status - CHỈ trả về events có status chính xác và chưa bị xóa
    @Query("SELECT e FROM JlptEvent e WHERE e.status = :status AND e.deletedFlag = false")
    List<JlptEvent> findByStatusAndDeletedFlagFalse(@Param("status") JlptEventStatus status);

    // Lọc theo status + level - CHỈ trả về events có status chính xác và chưa bị xóa
    @Query("SELECT e FROM JlptEvent e WHERE e.status = :status AND e.level = :level AND e.deletedFlag = false")
    List<JlptEvent> findByStatusAndLevelAndDeletedFlagFalse(@Param("status") JlptEventStatus status, @Param("level") String level);

    // Lọc theo level
    List<JlptEvent> findByLevelAndDeletedFlagFalse(String level);
}
