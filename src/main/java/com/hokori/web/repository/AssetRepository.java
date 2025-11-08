package com.hokori.web.repository;

import com.hokori.web.Enum.AssetType;
import com.hokori.web.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Page<Asset> findByOwner_Id(Long ownerId, Pageable pageable);

    @Query("select a from Asset a where a.owner.id = :ownerId and a.type = :type")
    Page<Asset> findByOwnerAndType(@Param("ownerId") Long ownerId,
                                   @Param("type") AssetType type,
                                   Pageable pageable);
}

