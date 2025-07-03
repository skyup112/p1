package com.example.p1.dto;

import lombok.*;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameScheduleDTO {
    private Long id;
    private String gameDate;
    private String opponent;
    private String location;
    private int homeScore;
    private int awayScore;
}
