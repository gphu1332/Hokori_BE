package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart",
        uniqueConstraints = @UniqueConstraint(name="uk_cart_user_open", columnNames={"user_id"}))
@Data
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)                 // user_id FK
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name="created_at", updatable = false)  private LocalDateTime createdAt;
    @Column(name="updated_at")                     private LocalDateTime updatedAt;

    @OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<CartItem> items = new ArrayList<>();

    @PrePersist void preP(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preU(){ updatedAt = LocalDateTime.now(); }


}

