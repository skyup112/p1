package com.example.p1.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "MEMBER")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq_gen") // GENERATOR 추가
    @SequenceGenerator(name = "member_seq_gen", sequenceName = "MEMBER_SEQ", allocationSize = 1) // SEQUENCE_NAME 정의
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // 유저 ID (로그인 ID)

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name; // 이름

    @Column(nullable = false, unique = true)
    private String nickname; // 닉네임

    @Column(nullable = false, unique = true)
    private String email; // 이메일

    @Column(nullable = false, unique = true)
    private String phoneNumber; // 휴대전화번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    private boolean banned = false;

    private LocalDateTime bannedUntil;

    public enum Role {
        USER, ADMIN
    }
}