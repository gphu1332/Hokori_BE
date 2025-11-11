package com.hokori.web.repository;

import com.hokori.web.entity.UserCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCertificateRepository extends JpaRepository<UserCertificate, Long> {
    List<UserCertificate> findByUser_Id(Long userId);
}
