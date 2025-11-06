package com.hokori.web.repository;

import com.hokori.web.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // ĐÚNG: đi theo quan hệ -> Course.Id
    Optional<CartItem> findByCart_IdAndCourse_Id(Long cartId, Long courseId);

    @EntityGraph(attributePaths = {"course"})
    List<CartItem> findByCart_Id(Long cartId);

    @Query("select coalesce(sum(ci.totalPrice),0) from CartItem ci " +
            "where ci.cart.id=:cartId and ci.selected=true")
    long sumSelectedTotal(@Param("cartId") Long cartId);
}

