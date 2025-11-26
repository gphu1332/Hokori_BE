package com.hokori.web.dto.jlpt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JlptTestListItemResponse {

    private Long id;

    // Tiêu đề hiển thị cho learner (BE tự generate)
    private String title;

    // N1 / N2 / N3 / N4 / N5
    private String level;

    // Thời gian làm bài (phút)
    private Integer durationMin;

    // Tổng điểm tối đa của đề (thường 180)
    private Integer totalScore;

    // Điểm tối thiểu để đậu (tính theo level)
    private Double passScore;

    // Số learner đã/đang làm đề này
    private Integer currentParticipants;
}
