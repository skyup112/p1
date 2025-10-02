package com.example.p1.repository;

import com.example.p1.domain.Comment;
import com.example.p1.domain.CommentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게임 ID의 댓글을 페이징 처리하여 조회 (최신순 정렬)
    // Pageable 객체를 인자로 받음
    Page<Comment> findByGameIdOrderByCreatedAtDesc(Long gameId, Pageable pageable);

    // 예측 댓글 존재 여부 확인 (addComment에서 사용)
    boolean existsByGameIdAndMemberUsernameAndType(Long gameId, String username, CommentType type);

    // 예측 카운트 조회 쿼리 (getPredictionCommentCounts에서 사용)
    @Query("SELECT c.predictedTeam.id, COUNT(c) FROM Comment c WHERE c.game.id = :gameId AND c.type = :type GROUP BY c.predictedTeam.id")
    List<Object[]> countPredictionsByGameId(Long gameId, CommentType type);
}