package com.minidoodle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDTO {
    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
