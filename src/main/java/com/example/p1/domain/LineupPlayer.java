package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "LINEUP_PLAYER")
@ToString(exclude = {"gameLineup"}) // 순환 참조 방지를 위해 추가
@EqualsAndHashCode(exclude = {"gameLineup"}) // 순환 참조 방지를 위해 추가
public class LineupPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lineup_player_seq_gen")
    @SequenceGenerator(name = "lineup_player_seq_gen", sequenceName = "LINEUP_PLAYER_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineup_id", nullable = false)
    private GameLineup gameLineup;

    // 타자의 타순. 투수는 0 또는 null로 처리
    @Column(nullable = false) // 필수 값으로 유지 (투수는 0으로 저장)
    private int orderNumber;

    @Column(nullable = false, length = 100)
    private String playerName;

    // 타자의 수비 포지션 (예: "1루수", "유격수", "지명타자"), 투수의 경우 "투수"로 저장
    @Column(nullable = false, length = 50) // 필수 값으로 유지 (투수는 "투수"로 저장)
    private String position;

    // 선수의 역할 (BATTER, PITCHER)을 명확히 구분
    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열로 저장
    @Column(nullable = false, length = 20) // 역할은 필수
    private PlayerRole playerRole;

    // 투수의 경우 이닝 (예: "5.1", "7"), 타자의 경우 null
    @Column(length = 10)
    private String innings;

    // 양방향 관계 설정 편의 메서드 (GameLineup에서 호출)
    public void setGameLineup(GameLineup gameLineup) {
        this.gameLineup = gameLineup;
    }
}