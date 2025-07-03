package com.example.p1.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {
    private String username; // 유저 ID
    private String password;
    private String name;     // 이름
    private String nickname; // 닉네임
    private String email;    // 이메일
    private String phoneNumber; // 휴대전화번호
}
