package com.example.p1.repository;

import com.example.p1.domain.TeamRanking;
import com.example.p1.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TeamRanking Repository Interface
 * TeamRanking 엔티티에 대한 데이터베이스 CRUD 작업을 처리합니다.
 */
@Repository
public interface TeamRankingRepository extends JpaRepository<TeamRanking, Long> {

    /**
     * 특정 시즌(연도)의 모든 팀 순위 정보를 조회합니다.
     * 현재 순위(currentRank) 오름차순으로 정렬됩니다.
     * @param seasonYear 조회할 시즌 연도
     * @return 해당 시즌의 TeamRanking 리스트
     */
    List<TeamRanking> findBySeasonYearOrderByCurrentRankAsc(int seasonYear);

    /**
     * 특정 팀의 특정 시즌(연도) 순위 정보를 조회합니다.
     * @param team 조회할 팀 엔티티
     * @param seasonYear 조회할 시즌 연도
     * @return 해당 팀의 해당 시즌 TeamRanking (존재하지 않을 경우 Optional.empty())
     */
    Optional<TeamRanking> findByTeamAndSeasonYear(Team team, int seasonYear);

    /**
     * 특정 시즌(연도)에 특정 팀 ID를 가진 팀 순위 정보가 존재하는지 확인합니다.
     * @param teamId 팀 ID
     * @param seasonYear 시즌 연도
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByTeamIdAndSeasonYear(Long teamId, int seasonYear);
}
