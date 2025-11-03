package com.hokori.web.entity;

import com.hokori.web.Enum.ContentFormat;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Entity @Table(name="sections_content")
@Getter @Setter
public class SectionsContent extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sections_id", nullable=false)
    private Section section;

    @Column(nullable=false) private Integer orderIndex = 0;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ContentFormat contentFormat = ContentFormat.ASSET;

    // đánh dấu nội dung chính
    @Column(nullable=false) private boolean primaryContent = false;

    // payload theo format
    private Long assetId;                         // ASSET (video/pdf/…)
    @Column(columnDefinition="TEXT") private String richText; // RICH_TEXT
    private Long quizId;                          // QUIZ_REF (nếu dùng)
    private Long flashcardSetId;                  // chỉ dùng nếu bạn chọn phương án B
}
