package com.hokori.web.entity;

import com.hokori.web.Enum.ContentFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sections_content")
@Getter
@Setter
public class SectionsContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sections_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private Integer orderIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentFormat contentFormat = ContentFormat.ASSET;

    // đánh dấu nội dung chính (ví dụ video chính của lesson)
    @Column(nullable = false)
    private boolean primaryContent = false;

    @Column(name = "file_path", length = 500)
    private String filePath;      // dùng khi contentFormat = ASSET

    @Column(columnDefinition = "TEXT")
    private String richText;      // dùng khi contentFormat = RICH_TEXT

    // chỉ dùng nếu bạn chọn phương án B cho VOCAB (section riêng dùng flashcardSetId)
    private Long flashcardSetId;

    // dùng khi contentFormat = QUIZ, reference đến Quiz entity
    private Long quizId;

    @Column(name = "is_trackable", nullable = false)
    private Boolean isTrackable = true;
}
