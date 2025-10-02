package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GAME_LINEUP")
public class GameLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_lineup_seq_gen")
    @SequenceGenerator(name = "game_lineup_seq_gen", sequenceName = "GAME_LINEUP_SEQ", allocationSize = 1)
    private Long id;

    // FIX: OneToOne에서 ManyToOne으로 변경하고, unique = true 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false) // unique = true 제거
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private GameSchedule game;


    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false, length = 10) // e.g., 'HOME', 'AWAY'
    private TeamType teamType;

    @OneToMany(mappedBy = "gameLineup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderNumber ASC")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<LineupPlayer> players = new ArrayList<>();

             public void setGame(GameSchedule game) {

                  this.game = game;
             }

    public void addPlayer(LineupPlayer player) {
        players.add(player);
        player.setGameLineup(this);
    }

    public void removePlayer(LineupPlayer player) {
        players.remove(player);
        player.setGameLineup(null);
    }
}
