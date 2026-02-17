package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import com.minidoodle.domain.User;
import com.minidoodle.dto.CreateTimeSlotRequest;
import com.minidoodle.dto.TimeSlotDTO;
import com.minidoodle.exception.BusinessException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.exception.SlotConflictException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private Calendar calendar;
    private TimeSlot timeSlot;
    private CreateTimeSlotRequest request;

    @BeforeEach
    void setUp() {
        User user = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        calendar = Calendar.builder()
            .id(1L)
            .user(user)
            .timezone("UTC")
            .build();

        timeSlot = TimeSlot.builder()
            .id(1L)
            .calendar(calendar)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
            .status(SlotStatus.FREE)
            .build();

        request = CreateTimeSlotRequest.builder()
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
            .build();
    }

    @Test
    void createTimeSlot_Success() {
        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));
        when(timeSlotRepository.existsOverlappingSlot(any(), any(), any(), any())).thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(timeSlot);

        TimeSlotDTO result = timeSlotService.createTimeSlot(1L, request);

        assertNotNull(result);
        assertEquals(SlotStatus.FREE, result.getStatus());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void createTimeSlot_CalendarNotFound_ThrowsException() {
        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> timeSlotService.createTimeSlot(1L, request));
    }

    @Test
    void createTimeSlot_OverlappingSlot_ThrowsException() {
        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));
        when(timeSlotRepository.existsOverlappingSlot(any(), any(), any(), any())).thenReturn(true);

        assertThrows(SlotConflictException.class,
            () -> timeSlotService.createTimeSlot(1L, request));
    }

    @Test
    void createTimeSlot_WithDuration_Success() {
        CreateTimeSlotRequest requestWithDuration = CreateTimeSlotRequest.builder()
            .startTime(LocalDateTime.now().plusDays(1))
            .durationMinutes(60)
            .build();

        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));
        when(timeSlotRepository.existsOverlappingSlot(any(), any(), any(), any())).thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(timeSlot);

        TimeSlotDTO result = timeSlotService.createTimeSlot(1L, requestWithDuration);

        assertNotNull(result);
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void createTimeSlot_StartTimeInPast_ThrowsException() {
        CreateTimeSlotRequest pastRequest = CreateTimeSlotRequest.builder()
            .startTime(LocalDateTime.now().minusDays(1))
            .endTime(LocalDateTime.now().minusDays(1).plusHours(1))
            .build();

        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));

        assertThrows(BusinessException.class,
            () -> timeSlotService.createTimeSlot(1L, pastRequest));
    }

    @Test
    void createTimeSlot_InvalidDuration_ThrowsException() {
        CreateTimeSlotRequest shortRequest = CreateTimeSlotRequest.builder()
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusMinutes(10))
            .build();

        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));

        assertThrows(BusinessException.class,
            () -> timeSlotService.createTimeSlot(1L, shortRequest));
    }

    @Test
    void markSlotAsBusy_Success() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(timeSlot);

        TimeSlotDTO result = timeSlotService.markSlotAsBusy(1L);

        assertNotNull(result);
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void deleteTimeSlot_BookedSlot_ThrowsException() {
        timeSlot.setStatus(SlotStatus.BOOKED);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));

        assertThrows(BusinessException.class, () -> timeSlotService.deleteTimeSlot(1L));
        verify(timeSlotRepository, never()).delete(any(TimeSlot.class));
    }
}
