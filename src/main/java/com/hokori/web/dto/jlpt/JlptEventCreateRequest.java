// com.hokori.web.dto.jlpt.JlptEventCreateRequest.java
package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JlptEventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JlptEventCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String level;          // N5, N4, ...

    private String description;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    // optional: cho phép tạo ở trạng thái OPEN/DRAFT, nếu null thì DRAFT
    private JlptEventStatus status;
}
