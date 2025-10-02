package com.example.p1.service;

import com.example.p1.dto.CommentDTO;
import com.example.p1.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Pageable import 추가

import java.util.Map;

public interface CommentService {

    // 페이징 처리를 위해 Pageable 인자를 받고 Page<CommentDTO>를 반환하도록 변경
    Page<CommentDTO> getCommentsByGameId(Long gameId, Pageable pageable);

    CommentDTO addComment(Long gameId, String username, CommentDTO commentDTO);

    Map<String, Long> getPredictionCommentCounts(Long gameId);

    CommentDTO updateComment(Long commentId, String username, CommentDTO commentDTO);

    void deleteComment(Long commentId, String username, boolean isAdmin);
    

    // DTO 변환 메서드
    CommentDTO toDTO(Comment comment);
}