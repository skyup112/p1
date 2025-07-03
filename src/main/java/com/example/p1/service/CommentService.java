package com.example.p1.service;

import com.example.p1.dto.CommentDTO;
import com.example.p1.domain.CommentType; // CommentType 임포트 확인

import java.util.List;
import java.util.Map;

/**
 * 댓글 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
 */
public interface CommentService {

    /**
     * 새로운 댓글을 추가합니다.
     * @param gameId 댓글이 속할 게임의 ID
     * @param username 댓글 작성자의 사용자 이름
     * @param commentDTO 댓글 데이터 전송 객체 (predictedTeam 필드 포함 가능)
     * @return 추가된 댓글의 DTO
     */
    CommentDTO addComment(Long gameId, String username, CommentDTO commentDTO);

    /**
     * 특정 경기의 모든 댓글 목록을 조회합니다.
     * @param gameId 게임 ID
     * @return 해당 게임의 댓글 DTO 목록
     */
    List<CommentDTO> getCommentsByGameId(Long gameId);

    /**
     * 특정 경기의 예측 댓글 개수를 팀별로 조회합니다.
     * @param gameId 게임 ID
     * @return 예측 댓글의 팀별 총 개수 Map
     */
    Map<String, Long> getPredictionCommentCounts(Long gameId);

    /**
     * 특정 댓글을 수정합니다.
     * @param commentId 수정할 댓글의 ID
     * @param username 댓글을 수정하려는 사용자의 사용자 이름
     * @param commentDTO 수정할 댓글 데이터
     * @return 수정된 댓글의 DTO
     */
    CommentDTO updateComment(Long commentId, String username, CommentDTO commentDTO);

    /**
     * 특정 댓글을 삭제합니다.
     * @param commentId 삭제할 댓글의 ID
     * @param username 댓글을 삭제하려는 사용자의 사용자 이름
     * @param isAdmin 사용자가 관리자 권한을 가지고 있는지 여부
     */
    void deleteComment(Long commentId, String username, boolean isAdmin);
}
