package com.hokori.web.repository;

import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderCode(Long orderCode);
    
    Optional<Payment> findByOrderCodeAndUserId(Long orderCode, Long userId);
    
    boolean existsByOrderCode(Long orderCode);
}

