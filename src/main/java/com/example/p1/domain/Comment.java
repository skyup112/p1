package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GAME_COMMENT")
@ToString(exclude = {"member", "game", "predictedTeam"}) // Exclude predictedTeam as well to prevent loops
@EntityListeners(AuditingEntityListener.class)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_comment_seq_gen")
    @SequenceGenerator(name = "game_comment_seq_gen", sequenceName = "GAME_COMMENT_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", referencedColumnName = "ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GAME_ID", referencedColumnName = "ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private GameSchedule game;

    @Column(length = 500)
    private String commentText;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PREDICTED_TEAM_ID", referencedColumnName = "ID", nullable = true)
    @EqualsAndHashCode.Exclude
    private Team predictedTeam;
}