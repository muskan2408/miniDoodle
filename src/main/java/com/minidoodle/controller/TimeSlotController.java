package com.minidoodle.controller;

import com.minidoodle.domain.SlotStatus;
import com.minidoodle.dto.AvailabilityResponse;
import com.minidoodle.dto.CreateTimeSlotRequest;
import com.minidoodle.dto.TimeSlotDTO;
import com.minidoodle.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timeslots")
@RequiredArgsConstructor
@Tag(name = "Time Slot Management", description = "APIs for managing time slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping("/users/{userId}")
    @Operation(summary = "Create a new time slot for a user")
    public ResponseEntity<TimeSlotDTO> createTimeSlot(
        @PathVariable Long userId,
        @Valid @RequestBody CreateTimeSlotRequest request
    ) {
        TimeSlotDTO created = timeSlotService.createTimeSlot(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get time slot by ID")
    public ResponseEntity<TimeSlotDTO> getTimeSlot(@PathVariable Long id) {
        TimeSlotDTO timeSlot = timeSlotService.getTimeSlot(id);
        return ResponseEntity.ok(timeSlot);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update time slot")
    public ResponseEntity<TimeSlotDTO> updateTimeSlot(
        @PathVariable Long id,
        @Valid @RequestBody CreateTimeSlotRequest request
    ) {
        TimeSlotDTO updated = timeSlotService.updateTimeSlot(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete time slot")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update time slot status")
    public ResponseEntity<TimeSlotDTO> updateStatus(
        @PathVariable Long id,
        @RequestParam SlotStatus status
    ) {
        TimeSlotDTO updated = timeSlotService.updateSlotStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/mark-busy")
    @Operation(summary = "Mark time slot as busy")
    public ResponseEntity<TimeSlotDTO> markAsBusy(@PathVariable Long id) {
        TimeSlotDTO updated = timeSlotService.markSlotAsBusy(id);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/mark-free")
    @Operation(summary = "Mark time slot as free")
    public ResponseEntity<TimeSlotDTO> markAsFree(@PathVariable Long id) {
        TimeSlotDTO updated = timeSlotService.markSlotAsFree(id);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get all time slots for a user within a time range")
    public ResponseEntity<List<TimeSlotDTO>> getTimeSlots(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<TimeSlotDTO> slots = timeSlotService.getSlotsByUserAndTimeRange(userId, startTime, endTime);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/users/{userId}/availability")
    @Operation(summary = "Get user availability (free and busy slots) within a time range")
    public ResponseEntity<AvailabilityResponse> getAvailability(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        AvailabilityResponse availability = timeSlotService.getAvailability(userId, startTime, endTime);
        return ResponseEntity.ok(availability);
    }
}
