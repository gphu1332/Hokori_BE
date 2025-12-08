package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data 
@NoArgsConstructor
@AllArgsConstructor
public class ChapterRes {
    private Long id;
    private String title;
    private Integer orderIndex;
    private String summary;
    private Boolean isTrial; // Đánh dấu chapter học thử
    private List<LessonRes> lessons; // có thể để List.of() khi tạo mới
    
    // Constructor backward compatibility (không có isTrial)
    public ChapterRes(Long id, String title, Integer orderIndex, String summary, List<LessonRes> lessons) {
        this.id = id;
        this.title = title;
        this.orderIndex = orderIndex;
        this.summary = summary;
        this.isTrial = false; // Default false
        this.lessons = lessons;
    }
}
