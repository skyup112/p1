package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "TEAM_RANKING", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "season_year"}) // 팀과 시즌(연도) 조합은 유일해야 합니다.
})
@EntityListeners(AuditingEntityListener.class) // 생성 및 수정 시간 자동 기록을 위한 리스너
public class TeamRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_ranking_seq_gen")
    @SequenceGenerator(name = "team_ranking_seq_gen", sequenceName = "TEAM_RANKING_SEQ", allocationSize = 1)
    private Long id; // 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false) // 팀 엔티티와의 다대일 관계
    private Team team; // 해당 순위 정보의 팀

    @Column(name = "season_year", nullable = false)
    private int seasonYear; // 순위가 기록된 시즌 연도 (예: 2023)

    @Column(nullable = false)
    @Builder.Default
    private int wins = 0; // 승리 횟수

    @Column(nullable = false)
    @Builder.Default
    private int losses = 0; // 패배 횟수

    @Column(nullable = false)
    @Builder.Default
    private int draws = 0; // 무승부 횟수

    @Column(nullable = false)
    @Builder.Default
    private double winRate = 0.0; // 승률 (예: 0.650)

    @Column(name = "current_rank", nullable = false) // 'rank' 대신 'current_rank'로 변경하여 현재 랭킹임을 명확히 합니다.
    @Builder.Default
    private int currentRank = 0; // 해당 시즌의 현재 순위

    @Column(name = "games_behind", nullable = false) // 1등 팀과의 게임차
    @Builder.Default
    private double gamesBehind = 0.0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 마지막 수정 일시
}

