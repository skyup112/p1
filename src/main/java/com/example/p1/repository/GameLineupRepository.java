 package com.example.p1.repository;

import com.example.p1.domain.GameLineup;
import com.example.p1.domain.TeamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameLineupRepository extends JpaRepository<GameLineup, Long> {

    // ADDED: To find a specific lineup (HOME or AWAY) for a game
    Optional<GameLineup> findByGameIdAndTeamType(Long gameId, TeamType teamType);

    // MODIFIED: Use FETCH JOIN to load players eagerly for both HOME and AWAY lineups
    @Query("SELECT gl FROM GameLineup gl JOIN FETCH gl.players p WHERE gl.game.id = :gameId")
    List<GameLineup> findAllByGameIdWithPlayers(@Param("gameId") Long gameId);

    // Kept: For cascade deletion from game_schedule (deletes all associated lineups)
    void deleteByGameId(Long gameId);
}