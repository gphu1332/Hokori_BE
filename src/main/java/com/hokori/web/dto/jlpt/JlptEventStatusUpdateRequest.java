package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JlptEventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JlptEventStatusUpdateRequest {
    @NotNull
    private JlptEventStatus status;
}
