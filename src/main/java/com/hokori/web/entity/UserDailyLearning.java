package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_daily_learning",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_daily_learning_user_date",
                columnNames = {"user_id", "learning_date"}
        )
)
@Getter
@Setter
@ToString(exclude = {"user"}) // Exclude relationships để tránh LazyInitializationException
public class UserDailyLearning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_daily_learning_user")
    )
    private User user;

    // Convenience methods để không break code hiện tại
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        if (userId != null) {
            this.user = new User();
            this.user.setId(userId);
        } else {
            this.user = null;
        }
    }

    @Column(name = "learning_date", nullable = false)
    private LocalDate learningDate;

    @Column(name = "activity_count", nullable = false)
    private Integer activityCount = 0;
}
