package com.example.p1.controller;

import com.example.p1.dto.CommentDTO;
import com.example.p1.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Pageable import 추가
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 댓글 관련 API 요청을 처리하는 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/games/{gameId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 새로운 댓글을 추가합니다.
     * @param gameId 댓글이 속할 게임의 ID
     * @param commentDTO 댓글 데이터 전송 객체 (predictedTeamName 필드 포함 가능)
     * @return 추가된 댓글의 DTO와 HTTP 상태 (201 Created, 409 Conflict, 404 Not Found, 500 Internal Server Error)
     */
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long gameId, @RequestBody CommentDTO commentDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            CommentDTO newComment = commentService.addComment(gameId, username, commentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 특정 경기의 모든 댓글 목록을 조회합니다. (페이징 적용)
     * @param gameId 게임 ID
     * @param pageable Spring이 자동으로 생성하는 Pageable 객체
     * @return 해당 게임의 댓글 DTO 목록 (Page 객체)와 HTTP 상태 (200 OK)
     */
    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getCommentsByGameId(
            @PathVariable Long gameId,
            Pageable pageable) { // Pageable 객체를 직접 인자로 받음

        Page<CommentDTO> comments = commentService.getCommentsByGameId(gameId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 경기의 예측 댓글 개수를 팀별로 조회합니다.
     * @param gameId 게임 ID
     * @return 예측 댓글의 팀별 총 개수 Map과 HTTP 상태 (200 OK)
     */
    @GetMapping("/prediction-counts")
    public ResponseEntity<Map<String, Long>> getPredictionCommentCounts(@PathVariable Long gameId) {
        Map<String, Long> counts = commentService.getPredictionCommentCounts(gameId);
        return ResponseEntity.ok(counts);
    }

    /**
     * 특정 댓글을 수정합니다.
     * @param gameId 댓글이 속한 게임의 ID
     * @param commentId 수정할 댓글의 ID
     * @param commentDTO 수정할 댓글 데이터
     * @return 수정된 댓글의 DTO와 HTTP 상태 (200 OK, 403 Forbidden, 404 Not Found, 400 Bad Request)
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long gameId,
            @PathVariable Long commentId,
            @RequestBody CommentDTO commentDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            CommentDTO updatedComment = commentService.updateComment(commentId, username, commentDTO);
            return ResponseEntity.ok(updatedComment);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * 특정 댓글을 삭제합니다.
     * @param gameId 댓글이 속한 게임의 ID
     * @param commentId 삭제할 댓글의 ID
     * @return HTTP 상태 (204 No Content, 403 Forbidden, 404 Not Found)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long gameId, @PathVariable Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        // 사용자가 ADMIN 역할을 가지고 있는지 확인
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            commentService.deleteComment(commentId, username, isAdmin);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}