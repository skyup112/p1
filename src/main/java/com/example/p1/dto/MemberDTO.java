package com.example.p1.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private Long id;
    private String username;
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean banned;
    private LocalDateTime bannedUntil;
}
