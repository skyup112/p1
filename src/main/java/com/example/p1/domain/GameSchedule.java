package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GAME_SCHEDULE")
@EntityListeners(AuditingEntityListener.class)
public class GameSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_schedule_seq_gen")
    @SequenceGenerator(name = "game_schedule_seq_gen", sequenceName = "GAME_SCHEDULE_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime gameDate;

    // Add the gameKey field here
    @Column(nullable = false, unique = true, length = 50) // gameKey는 고유하고, 비어있을 수 없으며, 적절한 길이로 설정
    private String gameKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "HOME_TEAM_ID", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "OPPONENT_TEAM_ID", nullable = false)
    private Team opponentTeam;

    @Column(nullable = false)
    private String location;

    @Builder.Default
    @Column(nullable = false)
    private int homeScore = 0;

    @Builder.Default
    @Column(nullable = false)
    private int awayScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GameStatus status = GameStatus.SCHEDULED;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GameLineup> lineups = new HashSet<>();

    // No changes needed for removeHtmlTags method itself
    public String removeHtmlTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("").trim();
    }
}