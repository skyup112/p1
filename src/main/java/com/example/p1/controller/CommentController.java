package com.example.p1.controller;

import com.example.p1.dto.CommentDTO;
import com.example.p1.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // Map 임포트

/**
 * 댓글 관련 API 요청을 처리하는 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/games/{gameId}/comments") // 기본 경로 설정
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 주입합니다.
public class CommentController {

    private final CommentService commentService;

    /**
     * 새로운 댓글을 추가합니다.
     * @param gameId 댓글이 속할 게임의 ID
     * @param commentDTO 댓글 데이터 전송 객체 (predictedTeam 필드 포함 가능)
     * @return 추가된 댓글의 DTO와 HTTP 상태 (201 Created, 409 Conflict, 404 Not Found, 500 Internal Server Error)
     */
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long gameId, @RequestBody CommentDTO commentDTO) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            CommentDTO newComment = commentService.addComment(gameId, username, commentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newComment); // 201 Created 반환
        } catch (IllegalStateException e) {
            // 예측 댓글이 이미 등록된 경우 (예: "이미 예측을 등록했습니다.")
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 409 Conflict 반환
        } catch (IllegalArgumentException e) {
            // 경기 또는 사용자를 찾을 수 없는 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found 반환
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 500 Internal Server Error 반환
        }
    }

    /**
     * 특정 경기의 모든 댓글 목록을 조회합니다.
     * @param gameId 게임 ID
     * @return 해당 게임의 댓글 DTO 목록과 HTTP 상태 (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<CommentDTO>> getCommentsByGameId(@PathVariable Long gameId) {
        List<CommentDTO> comments = commentService.getCommentsByGameId(gameId);
        return ResponseEntity.ok(comments); // 200 OK 반환
    }

    /**
     * 특정 경기의 예측 댓글 개수를 팀별로 조회합니다. - 다시 변경된 엔드포인트
     * @param gameId 게임 ID
     * @return 예측 댓글의 팀별 총 개수 Map과 HTTP 상태 (200 OK)
     */
    @GetMapping("/prediction-counts") // 엔드포인트 경로 변경
    public ResponseEntity<Map<String, Long>> getPredictionCommentCounts(@PathVariable Long gameId) { // 반환 타입 Map<String, Long>으로 변경
        Map<String, Long> counts = commentService.getPredictionCommentCounts(gameId);
        return ResponseEntity.ok(counts); // 200 OK 반환
    }

    /**
     * 특정 댓글을 수정합니다.
     * @param gameId 댓글이 속한 게임의 ID (경로 일치용)
     * @param commentId 수정할 댓글의 ID
     * @param commentDTO 수정할 댓글 데이터
     * @return 수정된 댓글의 DTO와 HTTP 상태 (200 OK, 403 Forbidden, 404 Not Found)
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
            return ResponseEntity.ok(updatedComment); // 200 OK 반환
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden 반환
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found 반환
        }
    }

    /**
     * 특정 댓글을 삭제합니다.
     * @param gameId 댓글이 속한 게임의 ID (경로 일치용)
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
            return ResponseEntity.noContent().build(); // 204 No Content 반환
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden 반환
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found 반환
        }
    }
}
