package com.hokori.web.repository;

import com.hokori.web.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderCode(Long orderCode);
    
    Optional<Payment> findByOrderCodeAndUserId(Long orderCode, Long userId);
    
    boolean existsByOrderCode(Long orderCode);
    
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<Payment> findByIdAndUserId(Long id, Long userId);
}

