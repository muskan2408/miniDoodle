package com.minidoodle.service;

import com.minidoodle.domain.Meeting;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import com.minidoodle.domain.User;
import com.minidoodle.dto.CreateMeetingRequest;
import com.minidoodle.dto.MeetingDTO;
import com.minidoodle.dto.UserDTO;
import com.minidoodle.exception.BusinessException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.MeetingRepository;
import com.minidoodle.repository.TimeSlotRepository;
import com.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;

    @Transactional
    public MeetingDTO createMeeting(CreateMeetingRequest request) {
        log.info("Creating meeting for time slot: {}", request.getTimeSlotId());

        // Use pessimistic lock to prevent concurrent booking
        TimeSlot timeSlot = timeSlotRepository.findByIdWithLock(request.getTimeSlotId())
            .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + request.getTimeSlotId()));

        // Validate time slot is available
        if (timeSlot.getStatus() != SlotStatus.FREE) {
            throw new BusinessException("Time slot is not available for booking");
        }

        // Check if meeting already exists for this slot
        if (meetingRepository.findByTimeSlotId(timeSlot.getId()).isPresent()) {
            throw new BusinessException("Meeting already exists for this time slot");
        }

        // Load participants
        Set<User> participants = new HashSet<>();
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            participants = request.getParticipantIds().stream()
                .map(id -> userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id)))
                .collect(Collectors.toSet());
        }

        // Create meeting
        Meeting meeting = Meeting.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .timeSlot(timeSlot)
            .participants(participants)
            .build();

        // Update time slot status
        timeSlot.setStatus(SlotStatus.BOOKED);
        timeSlotRepository.save(timeSlot);

        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("Created meeting with ID: {}", savedMeeting.getId());

        return mapToDTO(savedMeeting);
    }

    @Transactional(readOnly = true)
    public MeetingDTO getMeeting(Long id) {
        Meeting meeting = meetingRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));
        return mapToDTO(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsByUser(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Getting meetings for user {} between {} and {}", userId, startTime, endTime);

        List<Meeting> meetings = meetingRepository.findByParticipantIdAndTimeRange(userId, startTime, endTime);
        return meetings.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsByOwner(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Getting meetings owned by user {} between {} and {}", userId, startTime, endTime);

        List<Meeting> meetings = meetingRepository.findByOwnerIdAndTimeRange(userId, startTime, endTime);
        return meetings.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public MeetingDTO updateMeeting(Long id, CreateMeetingRequest request) {
        log.info("Updating meeting: {}", id);

        Meeting meeting = meetingRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());

        // Update participants if provided
        if (request.getParticipantIds() != null) {
            Set<User> newParticipants = request.getParticipantIds().stream()
                .map(userId -> userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId)))
                .collect(Collectors.toSet());

            meeting.getParticipants().clear();
            meeting.getParticipants().addAll(newParticipants);
        }

        Meeting updatedMeeting = meetingRepository.save(meeting);
        log.info("Updated meeting with ID: {}", id);
        return mapToDTO(updatedMeeting);
    }

    @Transactional
    public void cancelMeeting(Long id) {
        log.info("Cancelling meeting: {}", id);

        Meeting meeting = meetingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));

        // Free up the time slot
        TimeSlot timeSlot = meeting.getTimeSlot();
        timeSlot.setStatus(SlotStatus.FREE);
        timeSlotRepository.save(timeSlot);

        // Delete the meeting
        meetingRepository.delete(meeting);
        log.info("Cancelled meeting with ID: {}", id);
    }

    @Transactional
    public MeetingDTO addParticipant(Long meetingId, Long userId) {
        log.info("Adding participant {} to meeting {}", userId, meetingId);

        Meeting meeting = meetingRepository.findByIdWithParticipants(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        meeting.addParticipant(user);
        Meeting updatedMeeting = meetingRepository.save(meeting);

        return mapToDTO(updatedMeeting);
    }

    @Transactional
    public MeetingDTO removeParticipant(Long meetingId, Long userId) {
        log.info("Removing participant {} from meeting {}", userId, meetingId);

        Meeting meeting = meetingRepository.findByIdWithParticipants(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        meeting.removeParticipant(user);
        Meeting updatedMeeting = meetingRepository.save(meeting);

        return mapToDTO(updatedMeeting);
    }

    private MeetingDTO mapToDTO(Meeting meeting) {
        Set<UserDTO> participantDTOs = meeting.getParticipants().stream()
            .map(user -> UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build())
            .collect(Collectors.toSet());

        return MeetingDTO.builder()
            .id(meeting.getId())
            .title(meeting.getTitle())
            .description(meeting.getDescription())
            .timeSlotId(meeting.getTimeSlot().getId())
            .startTime(meeting.getTimeSlot().getStartTime())
            .endTime(meeting.getTimeSlot().getEndTime())
            .participants(participantDTOs)
            .participantIds(meeting.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet()))
            .createdAt(meeting.getCreatedAt())
            .updatedAt(meeting.getUpdatedAt())
            .build();
    }
}
