package com.minidoodle.repository;

import com.minidoodle.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    /**
     * Find meeting by time slot id
     */
    Optional<Meeting> findByTimeSlotId(Long timeSlotId);

    /**
     * Find all meetings for a user within a time range
     */
    @Query("SELECT DISTINCT m FROM Meeting m " +
           "JOIN m.participants p " +
           "WHERE p.id = :userId " +
           "AND m.timeSlot.startTime >= :startTime " +
           "AND m.timeSlot.endTime <= :endTime " +
           "ORDER BY m.timeSlot.startTime")
    List<Meeting> findByParticipantIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find meetings created by user (owner through calendar)
     */
    @Query("SELECT m FROM Meeting m " +
           "WHERE m.timeSlot.calendar.user.id = :userId " +
           "AND m.timeSlot.startTime >= :startTime " +
           "AND m.timeSlot.endTime <= :endTime " +
           "ORDER BY m.timeSlot.startTime")
    List<Meeting> findByOwnerIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find meeting with participants loaded
     */
    @Query("SELECT m FROM Meeting m LEFT JOIN FETCH m.participants WHERE m.id = :id")
    Optional<Meeting> findByIdWithParticipants(@Param("id") Long id);
}
