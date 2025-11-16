package com.hokori.web.repository;

import com.hokori.web.entity.JlptOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JlptOptionRepository extends JpaRepository<JlptOption, Long> {
    List<JlptOption> findByQuestion_IdOrderByOrderIndexAsc(Long questionId);
}
