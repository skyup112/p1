package com.example.p1.repository;

import com.example.p1.domain.GameSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameScheduleRepository extends JpaRepository<GameSchedule, Long> {

    // N+1 문제를 방지하기 위해 모든 연관 엔티티(homeTeam, opponentTeam, comments, lineups)를 FETCH JOIN으로 EAGER 로딩합니다.
    @Query("SELECT gs FROM GameSchedule gs " +
            "LEFT JOIN FETCH gs.homeTeam ht " +
            "LEFT JOIN FETCH gs.opponentTeam ot " +
            "LEFT JOIN FETCH gs.comments c " +
            "LEFT JOIN FETCH gs.lineups gl") // GameSchedule 엔티티의 'lineups' 필드와 매핑
    List<GameSchedule> findAll();

    // ID로 단일 경기 조회 시 N+1 문제를 방지하기 위해 모든 연관 엔티티를 FETCH JOIN으로 EAGER 로딩합니다.
    @Query("SELECT gs FROM GameSchedule gs " +
            "LEFT JOIN FETCH gs.homeTeam ht " +
            "LEFT JOIN FETCH gs.opponentTeam ot " +
            "LEFT JOIN FETCH gs.comments c " +
            "LEFT JOIN FETCH gs.lineups gl " + // GameSchedule 엔티티의 'lineups' 필드와 매핑
            "WHERE gs.id = :id")
    Optional<GameSchedule> findById(@Param("id") Long id);

    // 특정 기간(startDate부터 endDate까지)의 경기 일정을 조회하기 위한 메서드를 추가합니다.
    // 이 메서드는 크롤링된 데이터를 DB에 저장하거나 업데이트할 때 기존 데이터를 확인하는 데 사용됩니다.
    // 여기서는 연관 엔티티를 EAGER 로딩할 필요가 없을 수도 있습니다. (성능상 필요한 경우 FETCH JOIN 추가 고려)
    List<GameSchedule> findByGameDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}