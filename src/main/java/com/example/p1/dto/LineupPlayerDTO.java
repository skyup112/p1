package com.example.p1.dto;

import com.example.p1.domain.PlayerRole; // PlayerRole Enum 임포트
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineupPlayerDTO {
    private Long id;
    private String playerName;
    private Integer orderNumber; // 타자의 타순, 투수에게는 0 또는 null
    private String position; // 타자의 포지션 또는 투수
    private PlayerRole playerRole; // 선수의 역할 (BATTER, PITCHER)
    private String innings; // 투수의 이닝 (예: "5.1")
}