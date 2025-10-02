package com.example.p1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO will hold both home and away lineups for a single game.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineupResponseDTO {
    private Long gameId;
    private GameLineupDTO homeLineup;
    private GameLineupDTO awayLineup;
}