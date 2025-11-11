package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.*;

@Entity @Table(name="chapter")
@Getter @Setter
public class Chapter extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="course_id", nullable=false)
    private Course course;

    @Column(nullable=false) private String title;
    @Column(nullable=false) private Integer orderIndex = 0;
    private String summary;

    /** Đánh dấu chapter học thử (free preview). Mỗi course chỉ được 1 cái = true */
    @Column(nullable=false) private boolean isTrial = false;

    @OneToMany(mappedBy="chapter", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("orderIndex asc")
    private List<Lesson> lessons = new ArrayList<>();
}

