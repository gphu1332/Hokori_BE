package com.hokori.web.repository;

import com.hokori.web.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser_Id(Long userId); // 1 user 1 cart (mặc định OPEN)
}
