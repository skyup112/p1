package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GAME_COMMENT") // 테이블 이름은 계속 대문자로 유지
@ToString(exclude = {"member", "game"}) // 무한 루프 방지를 위해 관련 엔티티 제외
@EntityListeners(AuditingEntityListener.class) // Auditing 기능 활성화
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_comment_seq_gen") // GENERATOR 추가
    @SequenceGenerator(name = "game_comment_seq_gen", sequenceName = "GAME_COMMENT_SEQ", allocationSize = 1) // SEQUENCE_NAME 정의
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정으로 성능 최적화
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false) // NULLable 명시
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정으로 성능 최적화
    @JoinColumn(name = "game_id", referencedColumnName = "id", nullable = false)
    private GameSchedule game;

    @Column(length = 500) // 댓글 내용 길이 제한 (선택 사항)
    private String commentText; // 승패 예측 또는 일반 댓글 내용

    @CreatedDate // 엔티티 생성 시 자동으로 현재 시간 저장
    @Column(nullable = false, updatable = false) // 생성 시간은 필수이며 업데이트되지 않음
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티 업데이트 시 자동으로 현재 시간 저장
    @Column(nullable = true) // 업데이트 시간 필드를 nullable = true 로 변경
    private LocalDateTime updatedAt; // 업데이트 시간 필드

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // 댓글 종류는 필수
    private CommentType type; // 댓글 종류

    @Column(nullable = true) // 예측 댓글일 경우에만 값 가짐
    private String predictedTeam; // 예측 팀 (예: "롯데", "KIA")
}
