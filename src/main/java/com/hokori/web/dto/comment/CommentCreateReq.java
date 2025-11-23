package com.hokori.web.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateReq(
        @NotBlank
        @Size(max = 2000)
        String content
) {}