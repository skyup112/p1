package com.example.p1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDTO {
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
}

