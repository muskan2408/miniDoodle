package com.minidoodle.service;

import com.minidoodle.domain.*;
import com.minidoodle.dto.CreateMeetingRequest;
import com.minidoodle.dto.MeetingDTO;
import com.minidoodle.exception.BusinessException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.MeetingRepository;
import com.minidoodle.repository.TimeSlotRepository;
import com.minidoodle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    private TimeSlot timeSlot;
    private Meeting meeting;
    private User user1;
    private User user2;
    private CreateMeetingRequest request;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        user2 = User.builder()
            .id(2L)
            .name("Jane Smith")
            .email("jane@example.com")
            .build();

        Calendar calendar = Calendar.builder()
            .id(1L)
            .user(user1)
            .timezone("UTC")
            .build();

        timeSlot = TimeSlot.builder()
            .id(1L)
            .calendar(calendar)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
            .status(SlotStatus.FREE)
            .build();

        Set<User> participants = new HashSet<>();
        participants.add(user2);

        meeting = Meeting.builder()
            .id(1L)
            .title("Project Meeting")
            .description("Discuss project details")
            .timeSlot(timeSlot)
            .participants(participants)
            .build();

        Set<Long> participantIds = new HashSet<>();
        participantIds.add(2L);

        request = CreateMeetingRequest.builder()
            .timeSlotId(1L)
            .title("Project Meeting")
            .description("Discuss project details")
            .participantIds(participantIds)
            .build();
    }

    @Test
    void createMeeting_Success() {
        when(timeSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(timeSlot));
        when(meetingRepository.findByTimeSlotId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(timeSlot);

        MeetingDTO result = meetingService.createMeeting(request);

        assertNotNull(result);
        assertEquals("Project Meeting", result.getTitle());
        assertEquals(1, result.getParticipants().size());
        verify(meetingRepository).save(any(Meeting.class));
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void createMeeting_TimeSlotNotFound_ThrowsException() {
        when(timeSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> meetingService.createMeeting(request));
    }

    @Test
    void createMeeting_TimeSlotNotFree_ThrowsException() {
        timeSlot.setStatus(SlotStatus.BOOKED);
        when(timeSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(timeSlot));

        assertThrows(BusinessException.class,
            () -> meetingService.createMeeting(request));
    }

    @Test
    void createMeeting_MeetingAlreadyExists_ThrowsException() {
        when(timeSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(timeSlot));
        when(meetingRepository.findByTimeSlotId(1L)).thenReturn(Optional.of(meeting));

        assertThrows(BusinessException.class,
            () -> meetingService.createMeeting(request));
    }

    @Test
    void createMeeting_ParticipantNotFound_ThrowsException() {
        when(timeSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(timeSlot));
        when(meetingRepository.findByTimeSlotId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> meetingService.createMeeting(request));
    }

    @Test
    void getMeeting_Success() {
        when(meetingRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(meeting));

        MeetingDTO result = meetingService.getMeeting(1L);

        assertNotNull(result);
        assertEquals("Project Meeting", result.getTitle());
        assertEquals(1, result.getParticipants().size());
    }

    @Test
    void getMeeting_NotFound_ThrowsException() {
        when(meetingRepository.findByIdWithParticipants(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> meetingService.getMeeting(1L));
    }

    @Test
    void cancelMeeting_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(timeSlot);

        meetingService.cancelMeeting(1L);

        verify(timeSlotRepository).save(any(TimeSlot.class));
        verify(meetingRepository).delete(meeting);
    }

    @Test
    void addParticipant_Success() {
        when(meetingRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(3L)).thenReturn(Optional.of(
            User.builder().id(3L).name("New User").email("new@example.com").build()
        ));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        MeetingDTO result = meetingService.addParticipant(1L, 3L);

        assertNotNull(result);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void removeParticipant_Success() {
        when(meetingRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        MeetingDTO result = meetingService.removeParticipant(1L, 2L);

        assertNotNull(result);
        verify(meetingRepository).save(any(Meeting.class));
    }
}
