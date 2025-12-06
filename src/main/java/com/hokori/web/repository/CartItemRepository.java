package com.hokori.web.repository;

import com.hokori.web.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    /**
     * Update selected status for all items in a cart (native query to avoid loading entities).
     */
    @Modifying
    @Query(value = """
        UPDATE cartitem 
        SET is_selected = :selected, updated_at = CURRENT_TIMESTAMP
        WHERE cart_id = :cartId
        """, nativeQuery = true)
    void updateSelectedStatusByCartId(@Param("cartId") Long cartId, @Param("selected") boolean selected);
    
    /**
     * Delete all items in a cart (native query to avoid loading entities).
     */
    @Modifying
    @Query(value = """
        DELETE FROM cartitem 
        WHERE cart_id = :cartId
        """, nativeQuery = true)
    void deleteAllByCartId(@Param("cartId") Long cartId);
    
    /**
     * Delete cart items by cart ID and course IDs (native query to avoid loading entities).
     */
    @Modifying
    @Query(value = """
        DELETE FROM cartitem 
        WHERE cart_id = :cartId AND course_id IN :courseIds
        """, nativeQuery = true)
    void deleteByCart_IdAndCourse_IdIn(@Param("cartId") Long cartId, @Param("courseIds") List<Long> courseIds);
    
    /**
     * Get cart item metadata without loading Course entity (avoids LOB stream error).
     * Includes course status and deletedFlag for filtering invalid items.
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, courseId, quantity, totalPrice, selected, courseStatus, courseDeletedFlag]
     */
    @Query(value = """
        SELECT ci.id, ci.course_id, ci.quantity, ci.total_price, ci.is_selected, 
               c.status, c.deleted_flag
        FROM cartitem ci
        INNER JOIN course c ON ci.course_id = c.id
        WHERE ci.cart_id = :cartId
        ORDER BY ci.added_at ASC
        """, nativeQuery = true)
    List<Object[]> findCartItemMetadataByCartId(@Param("cartId") Long cartId);
    
    /**
     * Get cart item metadata by itemId without loading Course entity.
     * Returns: [id, courseId, cartId, quantity, totalPrice, selected]
     */
    @Query(value = """
        SELECT ci.id, ci.course_id, ci.cart_id, ci.quantity, ci.total_price, ci.is_selected
        FROM cartitem ci
        WHERE ci.id = :itemId
        """, nativeQuery = true)
    Optional<Object[]> findCartItemMetadataById(@Param("itemId") Long itemId);
}

