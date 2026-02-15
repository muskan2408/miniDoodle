package com.minidoodle.controller;

import com.minidoodle.dto.CreateMeetingRequest;
import com.minidoodle.dto.MeetingDTO;
import com.minidoodle.service.MeetingService;
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
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting Management", description = "APIs for managing meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @Operation(summary = "Create a new meeting from a time slot")
    public ResponseEntity<MeetingDTO> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
        MeetingDTO created = meetingService.createMeeting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meeting by ID")
    public ResponseEntity<MeetingDTO> getMeeting(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meeting")
    public ResponseEntity<MeetingDTO> updateMeeting(
        @PathVariable Long id,
        @Valid @RequestBody CreateMeetingRequest request
    ) {
        MeetingDTO updated = meetingService.updateMeeting(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel meeting")
    public ResponseEntity<Void> cancelMeeting(@PathVariable Long id) {
        meetingService.cancelMeeting(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get all meetings for a user (as participant) within a time range")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByUser(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<MeetingDTO> meetings = meetingService.getMeetingsByUser(userId, startTime, endTime);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/users/{userId}/owned")
    @Operation(summary = "Get all meetings owned by a user within a time range")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByOwner(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<MeetingDTO> meetings = meetingService.getMeetingsByOwner(userId, startTime, endTime);
        return ResponseEntity.ok(meetings);
    }

    @PostMapping("/{meetingId}/participants/{userId}")
    @Operation(summary = "Add participant to meeting")
    public ResponseEntity<MeetingDTO> addParticipant(
        @PathVariable Long meetingId,
        @PathVariable Long userId
    ) {
        MeetingDTO updated = meetingService.addParticipant(meetingId, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{meetingId}/participants/{userId}")
    @Operation(summary = "Remove participant from meeting")
    public ResponseEntity<MeetingDTO> removeParticipant(
        @PathVariable Long meetingId,
        @PathVariable Long userId
    ) {
        MeetingDTO updated = meetingService.removeParticipant(meetingId, userId);
        return ResponseEntity.ok(updated);
    }
}
