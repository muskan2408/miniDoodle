package com.minidoodle.repository;

import com.minidoodle.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByUserId(Long userId);

    @Query("SELECT c FROM Calendar c JOIN FETCH c.user WHERE c.user.id = :userId")
    Optional<Calendar> findByUserIdWithUser(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
