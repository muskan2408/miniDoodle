package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import com.minidoodle.dto.AvailabilityResponse;
import com.minidoodle.dto.CreateTimeSlotRequest;
import com.minidoodle.dto.TimeSlotDTO;
import com.minidoodle.exception.BusinessException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.exception.SlotConflictException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public TimeSlotDTO createTimeSlot(Long userId, CreateTimeSlotRequest request) {
        log.info("Creating time slot for user: {}", userId);

        Calendar calendar = calendarRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Calendar not found for user: " + userId));

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        // If duration is provided instead of end time, calculate end time
        if (request.getDurationMinutes() != null) {
            endTime = startTime.plusMinutes(request.getDurationMinutes());
        }

        validateTimeSlot(startTime, endTime);
        checkForOverlap(calendar.getId(), startTime, endTime, null);

        TimeSlot timeSlot = TimeSlot.builder()
            .calendar(calendar)
            .startTime(startTime)
            .endTime(endTime)
            .status(SlotStatus.FREE)
            .build();

        TimeSlot savedSlot = timeSlotRepository.save(timeSlot);
        log.info("Created time slot with ID: {}", savedSlot.getId());
        return mapToDTO(savedSlot);
    }

    @Transactional(readOnly = true)
    public TimeSlotDTO getTimeSlot(Long id) {
        TimeSlot timeSlot = timeSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));
        return mapToDTO(timeSlot);
    }

    @Transactional
    public TimeSlotDTO updateTimeSlot(Long id, CreateTimeSlotRequest request) {
        log.info("Updating time slot: {}", id);

        TimeSlot timeSlot = timeSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        if (timeSlot.getStatus() == SlotStatus.BOOKED) {
            throw new BusinessException("Cannot update a booked time slot");
        }

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (request.getDurationMinutes() != null) {
            endTime = startTime.plusMinutes(request.getDurationMinutes());
        }

        validateTimeSlot(startTime, endTime);
        checkForOverlap(timeSlot.getCalendar().getId(), startTime, endTime, id);

        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);

        TimeSlot updatedSlot = timeSlotRepository.save(timeSlot);
        log.info("Updated time slot with ID: {}", id);
        return mapToDTO(updatedSlot);
    }

    @Transactional
    public void deleteTimeSlot(Long id) {
        log.info("Deleting time slot: {}", id);

        TimeSlot timeSlot = timeSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        if (timeSlot.getStatus() == SlotStatus.BOOKED) {
            throw new BusinessException("Cannot delete a booked time slot. Cancel the meeting first.");
        }

        timeSlotRepository.delete(timeSlot);
        log.info("Deleted time slot with ID: {}", id);
    }

    @Transactional
    public TimeSlotDTO markSlotAsBusy(Long id) {
        return updateSlotStatus(id, SlotStatus.BUSY);
    }

    @Transactional
    public TimeSlotDTO markSlotAsFree(Long id) {
        return updateSlotStatus(id, SlotStatus.FREE);
    }

    @Transactional
    public TimeSlotDTO updateSlotStatus(Long id, SlotStatus status) {
        log.info("Updating slot {} status to: {}", id, status);

        TimeSlot timeSlot = timeSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        if (timeSlot.getStatus() == SlotStatus.BOOKED && status != SlotStatus.BOOKED) {
            throw new BusinessException("Cannot change status of a booked slot. Cancel the meeting first.");
        }

        timeSlot.setStatus(status);
        TimeSlot updatedSlot = timeSlotRepository.save(timeSlot);
        return mapToDTO(updatedSlot);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getSlotsByUserAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Calendar calendar = calendarRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Calendar not found for user: " + userId));

        List<TimeSlot> slots = timeSlotRepository.findByCalendarIdAndTimeRange(
            calendar.getId(), startTime, endTime);

        return slots.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Getting availability for user {} between {} and {}", userId, startTime, endTime);

        List<TimeSlotDTO> freeSlots = timeSlotRepository
            .findFreeSlotsByUserIdAndTimeRange(userId, startTime, endTime)
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        List<TimeSlotDTO> busySlots = timeSlotRepository
            .findBusySlotsByUserIdAndTimeRange(userId, startTime, endTime)
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return AvailabilityResponse.builder()
            .freeSlots(freeSlots)
            .busySlots(busySlots)
            .totalFreeSlots(freeSlots.size())
            .totalBusySlots(busySlots.size())
            .build();
    }

    private void validateTimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("Start time must be before end time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot create time slot in the past");
        }

        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < 15) {
            throw new BusinessException("Time slot duration must be at least 15 minutes");
        }

        if (durationMinutes > 480) {
            throw new BusinessException("Time slot duration cannot exceed 8 hours");
        }
    }

    private void checkForOverlap(Long calendarId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        boolean hasOverlap = timeSlotRepository.existsOverlappingSlot(calendarId, startTime, endTime, excludeId);
        if (hasOverlap) {
            throw new SlotConflictException("Time slot overlaps with an existing slot");
        }
    }

    private TimeSlotDTO mapToDTO(TimeSlot timeSlot) {
        return TimeSlotDTO.builder()
            .id(timeSlot.getId())
            .calendarId(timeSlot.getCalendar().getId())
            .startTime(timeSlot.getStartTime())
            .endTime(timeSlot.getEndTime())
            .status(timeSlot.getStatus())
            .durationMinutes(timeSlot.getDurationMinutes())
            .createdAt(timeSlot.getCreatedAt())
            .updatedAt(timeSlot.getUpdatedAt())
            .build();
    }
}
