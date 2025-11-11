package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.*;

@Entity @Table(name="lessons")
@Getter @Setter
public class Lesson extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="chapter_id", nullable=false)
    private Chapter chapter;

    @Column(nullable=false) private String title;
    @Column(nullable=false) private Integer orderIndex = 0;
    private Long totalDurationSec = 0L;

    @OneToMany(mappedBy="lesson", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("orderIndex asc")
    private List<Section> sections = new ArrayList<>();
}
