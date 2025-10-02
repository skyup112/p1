package com.example.p1.dto;

import com.example.p1.domain.TeamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameLineupDTO {
    private Long id;
    private Long gameId; // The ID of the game this lineup belongs to
    private TeamType teamType;
    private List<LineupPlayerDTO> players; // List of players in the lineup
}