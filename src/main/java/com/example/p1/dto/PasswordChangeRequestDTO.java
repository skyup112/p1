package com.example.p1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequestDTO {
    private String currentPassword; // 현재 비밀번호 확인용
    private String newPassword;     // 새 비밀번호
}