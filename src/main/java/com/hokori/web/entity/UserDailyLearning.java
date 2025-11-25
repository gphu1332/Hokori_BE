package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
public class UserDailyLearning extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "learning_date", nullable = false)
    private LocalDate learningDate;

    @Column(name = "activity_count", nullable = false)
    private Integer activityCount = 0;
}
