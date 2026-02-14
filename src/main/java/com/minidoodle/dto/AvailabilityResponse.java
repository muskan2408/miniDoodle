package com.minidoodle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private List<TimeSlotDTO> freeSlots;
    private List<TimeSlotDTO> busySlots;
    private int totalFreeSlots;
    private int totalBusySlots;
}
