package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data @AllArgsConstructor
public class ChapterRes {
    private Long id;
    private String title;
    private Integer orderIndex;
    private String summary;
    private List<LessonRes> lessons; // có thể để List.of() khi tạo mới
    // (nếu bạn có isTrial thì thêm Boolean trial ở đây)
}
