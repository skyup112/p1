package com.example.p1.service;

import com.example.p1.dto.CommentDTO;
import com.example.p1.domain.Comment;
import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.Member;
import com.example.p1.domain.CommentType;
import com.example.p1.domain.Team;
import com.example.p1.repository.CommentRepository;
import com.example.p1.repository.GameScheduleRepository;
import com.example.p1.repository.MemberRepository;
import com.example.p1.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Pageable import 추가
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
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final GameScheduleRepository gameScheduleRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public CommentDTO addComment(Long gameId, String username, CommentDTO commentDTO) {
        GameSchedule game = gameScheduleRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다: " + gameId));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        Team predictedTeam = null;

        if (commentDTO.getType() == CommentType.PREDICTION) {
            boolean hasExistingPrediction = commentRepository.existsByGameIdAndMemberUsernameAndType(gameId, username, CommentType.PREDICTION);
            if (hasExistingPrediction) {
                throw new IllegalStateException("이미 이 경기에 예측 댓글을 등록했습니다.");
            }
            if (commentDTO.getPredictedTeamName() == null || commentDTO.getPredictedTeamName().isEmpty()) {
                throw new IllegalArgumentException("예측 댓글은 예측 팀을 지정해야 합니다.");
            }
            predictedTeam = teamRepository.findByName(commentDTO.getPredictedTeamName());
            if (predictedTeam == null) {
                throw new IllegalArgumentException("예측 팀을 찾을 수 없습니다: " + commentDTO.getPredictedTeamName());
            }
        }

        Comment comment = Comment.builder()
                .game(game)
                .member(member)
                .commentText(commentDTO.getCommentText())
                .type(commentDTO.getType())
                .predictedTeam(predictedTeam)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return toDTO(savedComment);
    }

    // 페이징 처리된 댓글 목록 조회 메서드 구현
    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsByGameId(Long gameId, Pageable pageable) {
        // Pageable 객체를 직접 Repository에 전달하여 페이징 및 정렬을 처리합니다.
        Page<Comment> commentPage = commentRepository.findByGameIdOrderByCreatedAtDesc(gameId, pageable);

        // Page<Comment>를 Page<CommentDTO>로 변환하여 반환
        return commentPage.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getPredictionCommentCounts(Long gameId) {
        List<Object[]> results = commentRepository.countPredictionsByGameId(gameId, CommentType.PREDICTION);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            String teamName;
            if (result[0] instanceof Long) { // If repository returns Team ID
                Long teamId = (Long) result[0];
                Team team = teamRepository.findById(teamId).orElse(null);
                teamName = (team != null) ? team.getName() : "UNKNOWN_TEAM_ID_" + teamId;
            } else { // If repository returns Team Name (String)
                teamName = (String) result[0];
            }

            Long count = (Long) result[1];

            if (teamName == null) {
                teamName = "UNKNOWN_OR_NULL_TEAM";
            }
            counts.put(teamName, count);
        }
        return counts;
    }

    @Override
    @Transactional
    public CommentDTO updateComment(Long commentId, String username, CommentDTO commentDTO) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        Member currentUser = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // Check if the current user is the author of the comment
        boolean isAuthor = comment.getMember() != null && comment.getMember().equals(currentUser);

        // Check if the current user is an admin
        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().equals(com.example.p1.domain.Member.Role.ADMIN);

        // If not the author AND not an admin, throw SecurityException
        if (!isAuthor && !isAdmin) {
            throw new SecurityException("댓글을 수정할 권한이 없습니다.");
        }

        comment.setCommentText(commentDTO.getCommentText());

        if (commentDTO.getType() == CommentType.PREDICTION) {
            if (commentDTO.getPredictedTeamName() == null || commentDTO.getPredictedTeamName().isEmpty()) {
                throw new IllegalArgumentException("예측 댓글은 예측 팀을 지정해야 합니다.");
            }
            Team newPredictedTeam = teamRepository.findByName(commentDTO.getPredictedTeamName());
            if (newPredictedTeam == null) {
                throw new IllegalArgumentException("예측 팀을 찾을 수 없습니다: " + commentDTO.getPredictedTeamName());
            }
            comment.setPredictedTeam(newPredictedTeam);
        } else {
            // If the comment type is changed from PREDICTION to TEXT, clear predicted team
            comment.setPredictedTeam(null);
        }

        Comment updatedComment = commentRepository.save(comment);
        return toDTO(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String username, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        Member currentUser = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // Using equals for Member objects
        boolean isAuthor = comment.getMember() != null && comment.getMember().equals(currentUser);

        if (!isAuthor && !isAdmin) {
            throw new SecurityException("댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }

    @Override
    public CommentDTO toDTO(Comment comment) {
        String nickname = (comment.getMember() != null) ? comment.getMember().getNickname() : null;
        String username = (comment.getMember() != null) ? comment.getMember().getUsername() : null;
        Long gameId = (comment.getGame() != null) ? comment.getGame().getId() : null;
        String predictedTeamName = (comment.getPredictedTeam() != null) ? comment.getPredictedTeam().getName() : null;

        return CommentDTO.builder()
                .id(comment.getId())
                .gameId(gameId)
                .username(username)
                .commentText(comment.getCommentText())
                .type(comment.getType())
                .predictedTeamName(predictedTeamName)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .nickname(nickname)
                .build();
    }
}