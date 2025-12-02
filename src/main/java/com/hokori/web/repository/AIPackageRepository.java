package com.hokori.web.repository;

import com.hokori.web.entity.AIPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIPackageRepository extends JpaRepository<AIPackage, Long> {
    
    List<AIPackage> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<AIPackage> findAllByOrderByDisplayOrderAsc();
}

