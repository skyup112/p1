package com.example.p1.service;

import com.example.p1.domain.GameLineup;
import com.example.p1.dto.GameLineupDTO;
import com.example.p1.dto.LineupResponseDTO; // New import

public interface GameLineupService {
    // Modified: Now returns a combined DTO for home and away lineups
    LineupResponseDTO getLineupByGameId(Long gameId);

    // Signature remains the same, but implementation will use lineupDTO.getTeamType()
    GameLineupDTO createOrUpdateLineup(Long gameId, GameLineupDTO lineupDTO);

    void deleteLineupByGameId(Long gameId);

    // This remains to convert a single GameLineup entity to its DTO, including teamType
    GameLineupDTO toDTO(GameLineup entity);
}