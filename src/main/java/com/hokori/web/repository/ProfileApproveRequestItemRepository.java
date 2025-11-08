package com.hokori.web.repository;

import com.hokori.web.entity.ProfileApproveRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileApproveRequestItemRepository extends JpaRepository<ProfileApproveRequestItem, Long> {
    List<ProfileApproveRequestItem> findByRequest_Id(Long requestId);
}
