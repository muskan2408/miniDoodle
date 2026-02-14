package com.minidoodle.repository;

import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Find all time slots for a calendar within a time range
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findByCalendarIdAndTimeRange(
        @Param("calendarId") Long calendarId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find all time slots for a calendar with specific status within time range
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
           "AND ts.status = :status " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findByCalendarIdAndStatusAndTimeRange(
        @Param("calendarId") Long calendarId,
        @Param("status") SlotStatus status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Check if there are overlapping slots for a calendar
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
           "AND ((ts.startTime < :endTime AND ts.endTime > :startTime)) " +
           "AND (:excludeId IS NULL OR ts.id != :excludeId)")
    boolean existsOverlappingSlot(
        @Param("calendarId") Long calendarId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("excludeId") Long excludeId
    );

    /**
     * Find slot with pessimistic lock for concurrent booking
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);

    /**
     * Find all FREE slots for a user within a time range
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.user.id = :userId " +
           "AND ts.status = 'FREE' " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findFreeSlotsByUserIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find all BUSY/BOOKED slots for a user within a time range
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.user.id = :userId " +
           "AND ts.status IN ('BUSY', 'BOOKED') " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findBusySlotsByUserIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count slots by calendar and status
     */
    long countByCalendarIdAndStatus(Long calendarId, SlotStatus status);
}
