package com.example.p1.dto;

import com.example.p1.domain.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private Long id;
    private Long gameId;
    private String username;
    private String commentText;
    private CommentType type; // TEXT 또는 PREDICTION
    private String predictedTeam; // 예측 팀 (예: "롯데", "KIA") - 다시 추가된 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String nickname;
}
