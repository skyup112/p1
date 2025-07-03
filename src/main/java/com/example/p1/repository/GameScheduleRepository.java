package com.example.p1.repository;

import com.example.p1.domain.GameSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameScheduleRepository extends JpaRepository<GameSchedule, Long> {
    @Query("SELECT gs FROM GameSchedule gs LEFT JOIN FETCH gs.comments c WHERE gs.id = :id")
    Optional<GameSchedule> findByIdWithComments(@Param("id") Long id);
}
