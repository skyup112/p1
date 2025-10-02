// src/main/java/com/example/p1/domain/Team.java (최종 권장 버전)
package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "TEAM")

public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_seq_gen")
    @SequenceGenerator(name = "team_seq_gen", sequenceName = "TEAM_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // e.g., "롯데 자이언츠", "두산 베어스"

    @Column(nullable = false, length = 255)
    private String logoUrl; // URL or path to the logo image
}