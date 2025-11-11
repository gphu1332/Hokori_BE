package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// CartItem.java
// CartItem.java
@Entity
@Table(name = "cartitem",
        uniqueConstraints = @UniqueConstraint(name="uk_cart_course", columnNames={"cart_id","course_id"}))
@Data
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="cart_id", nullable=false)
    private Cart cart;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", nullable=false)   // <-- FK tới course
    private Course course;

    @Column(name="quantity",   nullable=false) private Integer quantity = 1;
    @Column(name="total_price",nullable=false) private Long totalPrice; // snapshot: price * qty
    @Column(name="is_selected",nullable=false) private Boolean selected = true;

    @Column(name="added_at",   updatable=false) private LocalDateTime addedAt;
    @Column(name="updated_at")                  private LocalDateTime updatedAt;

    @PrePersist void preP(){ addedAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preU(){ updatedAt = LocalDateTime.now(); }

    // tiện cho DTO
    public Long getCourseId(){ return course != null ? course.getId() : null; }

}

