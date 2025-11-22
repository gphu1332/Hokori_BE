package com.hokori.web.repository;

import com.hokori.web.entity.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {

    /**
     * Tìm file theo filePath (không bị xóa)
     */
    Optional<FileStorage> findByFilePathAndDeletedFlagFalse(String filePath);

    /**
     * Kiểm tra file đã tồn tại chưa (không bị xóa)
     */
    boolean existsByFilePathAndDeletedFlagFalse(String filePath);
}

