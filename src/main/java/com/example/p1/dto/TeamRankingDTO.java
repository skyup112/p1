package com.example.p1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRankingDTO {
    private Long id;
    private TeamDTO team; // Team 엔티티 대신 TeamDTO를 사용합니다.
    private int seasonYear;
    private int wins;
    private int losses;
    private int draws;
    private double winRate;
    private int currentRank;
    private double gamesBehind;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}