package com.example.p1.service;

import com.example.p1.domain.GameSchedule;
import com.example.p1.dto.GameScheduleDTO;
import com.example.p1.repository.CommentRepository;
import com.example.p1.repository.GameScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameScheduleServiceImpl implements GameScheduleService {

    private final GameScheduleRepository gameScheduleRepository;
    private final CommentRepository commentRepository;

    @Override
    public GameScheduleDTO createGame(GameScheduleDTO gameDTO) {
        GameSchedule game = toEntity(gameDTO);
        GameSchedule savedGame = gameScheduleRepository.save(game);
        return toDTO(savedGame);
    }

    @Override
    public List<GameScheduleDTO> getAllGames() {
        return gameScheduleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GameScheduleDTO getGame(Long id) {
        GameSchedule game = gameScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경기 없음: " + id));
        return toDTO(game);
    }

    @Override
    public GameScheduleDTO updateGame(Long id, GameScheduleDTO updatedGameDTO) {
        GameSchedule game = gameScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경기 없음: " + id));

        // DTO에서 받은 gameDate String을 LocalDateTime으로 직접 파싱
        try {
            // 프론트엔드에서 'YYYY-MM-DDTHH:mm' 형식의 문자열을 보내므로, LocalDateTime.parse()로 직접 파싱 가능
            game.setGameDate(LocalDateTime.parse(updatedGameDTO.getGameDate()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 경기 날짜 형식입니다: " + updatedGameDTO.getGameDate(), e);
        }

        game.setOpponent(updatedGameDTO.getOpponent());
        game.setLocation(updatedGameDTO.getLocation());
        game.setHomeScore(updatedGameDTO.getHomeScore());
        game.setAwayScore(updatedGameDTO.getAwayScore());

        GameSchedule savedGame = gameScheduleRepository.save(game);
        return toDTO(savedGame);
    }

    @Override
    public void deleteGame(Long id) {
        // 1. Check if the game exists. This is good practice.
        if (!gameScheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 경기 없음: " + id);
        }
        commentRepository.deleteByGameId(id);

        gameScheduleRepository.deleteById(id);
    }

    // --- DTO <-> Entity 변환 메서드 ---
    private GameScheduleDTO toDTO(GameSchedule game) {
        return GameScheduleDTO.builder()
                .id(game.getId())
                .gameDate(game.getGameDate().toString()) // LocalDateTime을 String으로 변환
                .opponent(game.getOpponent())
                .location(game.getLocation())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .build();
    }

    private GameSchedule toEntity(GameScheduleDTO dto) {
        // gameDate는 String으로 받아서 LocalDateTime으로 직접 파싱
        LocalDateTime parsedGameDate;
        try {
            // 프론트엔드에서 'YYYY-MM-DDTHH:mm' 형식의 문자열을 보내므로, LocalDateTime.parse()로 직접 파싱 가능
            parsedGameDate = LocalDateTime.parse(dto.getGameDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 경기 날짜 형식입니다: " + dto.getGameDate(), e);
        }

        return GameSchedule.builder()
                .id(dto.getId())
                .gameDate(parsedGameDate) // String을 LocalDateTime으로 변환하여 설정
                .opponent(dto.getOpponent())
                .location(dto.getLocation())
                .homeScore(dto.getHomeScore())
                .awayScore(dto.getAwayScore())
                .build();
    }
}
