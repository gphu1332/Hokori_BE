package com.hokori.web.repository;

import com.hokori.web.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByQuestion_IdOrderByOrderIndexAsc(Long questionId);
    long countByQuestion_IdAndIsCorrectTrue(Long questionId);
}

