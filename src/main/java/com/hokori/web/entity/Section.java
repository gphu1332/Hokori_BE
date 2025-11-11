package com.hokori.web.entity;

import com.hokori.web.Enum.ContentType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.*;

@Entity @Table(name="sections")
@Getter @Setter
public class Section extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="lesson_id", nullable=false)
    private Lesson lesson;

    @Column(nullable=false) private String title;
    @Column(nullable=false) private Integer orderIndex = 0;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ContentType studyType = ContentType.GRAMMAR; // GRAMMAR/VOCABULARY/KANJI

    // VOCAB: bắt buộc != null (phương án A)
    private Long flashcardSetId;

    @OneToMany(mappedBy="section", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("orderIndex asc")
    private List<SectionsContent> contents = new ArrayList<>();
}
