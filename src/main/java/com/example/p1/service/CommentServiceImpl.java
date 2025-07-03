package com.example.p1.service;

import com.example.p1.dto.CommentDTO;
import com.example.p1.domain.Comment;
import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.Member;
import com.example.p1.domain.CommentType;
import com.example.p1.repository.CommentRepository;
import com.example.p1.repository.GameScheduleRepository;
import com.example.p1.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직을 구현하는 서비스 구현체.
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService { // CommentService 인터페이스 구현

    private final CommentRepository commentRepository;
    private final GameScheduleRepository gameScheduleRepository;
    private final MemberRepository memberRepository;

    @Override // 인터페이스 메서드 구현 명시
    @Transactional
    public CommentDTO addComment(Long gameId, String username, CommentDTO commentDTO) {
        GameSchedule game = gameScheduleRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다: " + gameId));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        if (commentDTO.getType() == CommentType.PREDICTION) {
            boolean hasExistingPrediction = commentRepository.existsByGameIdAndMemberUsernameAndType(gameId, username, CommentType.PREDICTION);
            if (hasExistingPrediction) {
                throw new IllegalStateException("이미 이 경기에 예측 댓글을 등록했습니다.");
            }
            if (commentDTO.getPredictedTeam() == null || commentDTO.getPredictedTeam().isEmpty()) {
                throw new IllegalArgumentException("예측 댓글은 예측 팀을 지정해야 합니다.");
            }
        }

        Comment comment = Comment.builder()
                .game(game)
                .member(member)
                .commentText(commentDTO.getCommentText())
                .type(commentDTO.getType())
                .predictedTeam(commentDTO.getPredictedTeam())
                .build();

        Comment savedComment = commentRepository.save(comment);
        return convertToDto(savedComment);
    }

    @Override // 인터페이스 메서드 구현 명시
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByGameId(Long gameId) {
        List<Comment> comments = commentRepository.findByGameIdOrderByCreatedAtDesc(gameId);
        // 컨트롤러에서 닉네임 매핑을 처리하므로, 여기서는 Comment 엔티티 목록을 그대로 반환합니다.
        // 하지만 CommentController의 getCommentsByGameId 메서드가 stream().map(this::convertToDto)를 사용하고 있으므로,
        // convertToDto가 닉네임을 포함하도록 수정하는 것이 일관성 있습니다.
        return comments.stream()
                .map(this::convertToDto) // convertToDto가 닉네임을 포함하도록 수정됨
                .collect(Collectors.toList());
    }

    @Override // 인터페이스 메서드 구현 명시
    @Transactional(readOnly = true)
    public Map<String, Long> getPredictionCommentCounts(Long gameId) {
        List<Object[]> results = commentRepository.countPredictionsByGameId(gameId, CommentType.PREDICTION);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            String teamName = (String) result[0];
            Long count = (Long) result[1];

            // Handle null predictedTeam values gracefully
            if (teamName == null) {
                teamName = "UNKNOWN_OR_NULL_TEAM"; // Assign a default key for nulls
            }
            counts.put(teamName, count);
        }
        return counts;
    }

    @Override // 인터페이스 메서드 구현 명시
    @Transactional
    public CommentDTO updateComment(Long commentId, String username, CommentDTO commentDTO) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        // Note: For role comparison, ensure "ADMIN" is the correct string value from your Role enum.
        // It's generally safer to compare enum values directly if possible, e.g., member.getRole().equals(Role.ADMIN)
        // Assuming memberRepository.findByUsername(username).get().getRole() returns a String "ADMIN"
        if (!comment.getMember().getUsername().equals(username) && !memberRepository.findByUsername(username).get().getRole().toString().equals("ADMIN")) {
            throw new SecurityException("댓글을 수정할 권한이 없습니다.");
        }

        comment.setCommentText(commentDTO.getCommentText());
        Comment updatedComment = commentRepository.save(comment);
        return convertToDto(updatedComment);
    }

    @Override // 인터페이스 메서드 구현 명시
    @Transactional
    public void deleteComment(Long commentId, String username, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getMember().getUsername().equals(username) && !isAdmin) {
            throw new SecurityException("댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }

    // Private helper method to convert Comment entity to CommentDTO
    private CommentDTO convertToDto(Comment comment) {
        // Ensure the Member object is not null before accessing its properties
        String nickname = (comment.getMember() != null) ? comment.getMember().getNickname() : null;
        String username = (comment.getMember() != null) ? comment.getMember().getUsername() : null;
        Long gameId = (comment.getGame() != null) ? comment.getGame().getId() : null;

        return CommentDTO.builder()
                .id(comment.getId())
                .gameId(gameId) // GameSchedule ID 포함
                .username(username) // 작성자 username 포함
                .commentText(comment.getCommentText())
                .type(comment.getType())
                .predictedTeam(comment.getPredictedTeam())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .nickname(nickname) // <-- IMPORTANT: Now includes the nickname
                .build();
    }
}
