package com.hokori.web.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChapterUpsertReq {
    @NotBlank
    private String title;

    private Integer orderIndex;
    private String summary;

    // đánh dấu muốn tạo chapter học thử (optional)
    private Boolean isTrial;   // <== đặt tên 'trial' (không phải 'isTrial')
}
