package com.example.p1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameScheduleDTO {
    private Long id;
    private LocalDateTime gameDate;
    private String location;
    private Integer homeScore;
    private Integer awayScore;
    private String status;

    // --- Add gameKey here ---
    private String gameKey; // <-- This line needs to be added

    private TeamDTO homeTeam;
    private TeamDTO opponentTeam;

    private List<CommentDTO> comments;
    private List<GameLineupDTO> lineups;

    // FIX: fromEntity 정적 팩토리 메서드를 서비스 계층으로 이동하여 여기서는 제거합니다.
    // 변환 로직은 GameScheduleServiceImpl에서 담당합니다.
}