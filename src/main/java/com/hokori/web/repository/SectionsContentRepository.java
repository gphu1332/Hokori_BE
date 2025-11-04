// com/hokori/web/repository/SectionsContentRepository.java
package com.hokori.web.repository;

import com.hokori.web.entity.SectionsContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionsContentRepository extends JpaRepository<SectionsContent, Long> {
    long countBySection_Id(Long sectionId);

    List<SectionsContent> findBySection_IdOrderByOrderIndexAsc(Long sectionId);
}
