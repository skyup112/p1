package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet; // Set을 사용하기 위해 import
import java.util.Set; // Set을 사용하기 위해 import

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GAME_SCHEDULE") // 테이블 이름은 계속 대문자로 유지
public class GameSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_schedule_seq_gen") // GENERATOR 추가
    @SequenceGenerator(name = "game_schedule_seq_gen", sequenceName = "GAME_SCHEDULE_SEQ", allocationSize = 1) // SEQUENCE_NAME 정의
    private Long id;

    // LocalDate 대신 LocalDateTime으로 변경하여 시간 정보도 포함
    @Column(nullable = false)
    private LocalDateTime gameDate;

    @Column(nullable = false)
    private String opponent; // 상대팀

    @Column(nullable = false)
    private String location;

    @Builder.Default
    @Column(nullable = false)
    private int homeScore = 0; // 홈 팀 점수

    @Builder.Default
    @Column(nullable = false)
    private int awayScore = 0; // 원정 팀 점수

    // Comment 엔티티와의 One-to-Many 관계 설정
    // mappedBy: Comment 엔티티의 'game' 필드에 의해 매핑됨을 나타냅니다.
    // cascade = CascadeType.ALL: GameSchedule이 삭제될 때 연관된 Comment도 모두 삭제됩니다.
    // orphanRemoval = true: 부모(GameSchedule)에서 더 이상 참조되지 않는 자식(Comment)은 자동으로 삭제됩니다.
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // 빌더 패턴 사용 시 기본값 설정
    private Set<Comment> comments = new HashSet<>(); // 댓글 목록 (초기화)
}
