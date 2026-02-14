package com.minidoodle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {

    @NotNull(message = "Time slot ID is required")
    private Long timeSlotId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Set<Long> participantIds;
}
