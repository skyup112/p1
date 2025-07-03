package com.example.p1.repository;

import com.example.p1.domain.Comment;
import com.example.p1.domain.CommentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // IMPORTANT: Required for DML queries
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByGameIdOrderByCreatedAtDesc(Long gameId);
    boolean existsByGameIdAndMemberUsernameAndType(Long gameId, String username, CommentType type);

    @Query("SELECT c.predictedTeam, COUNT(c) FROM Comment c WHERE c.game.id = :gameId AND c.type = :type GROUP BY c.predictedTeam")
    List<Object[]> countPredictionsByGameId(@Param("gameId") Long gameId, @Param("type") CommentType type);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.game.id = :gameId")
    void deleteByGameId(@Param("gameId") Long gameId);
}