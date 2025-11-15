// com.hokori.web.repository.JlptEventRepository.java
package com.hokori.web.repository;

import com.hokori.web.Enum.JlptEventStatus;
import com.hokori.web.entity.JlptEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptEventRepository extends JpaRepository<JlptEvent, Long> {

    // Lấy tất cả event chưa bị xóa mềm
    List<JlptEvent> findByDeletedFlagFalse();

    // Lọc theo status
    List<JlptEvent> findByStatusAndDeletedFlagFalse(JlptEventStatus status);

    // Lọc theo status + level
    List<JlptEvent> findByStatusAndLevelAndDeletedFlagFalse(JlptEventStatus status, String level);

    // Lọc theo level
    List<JlptEvent> findByLevelAndDeletedFlagFalse(String level);
}
