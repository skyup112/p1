package com.example.p1.service;

import com.example.p1.domain.GameLineup;
import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.LineupPlayer;
import com.example.p1.domain.TeamType;
import com.example.p1.domain.PlayerRole;
import com.example.p1.dto.GameLineupDTO;
import com.example.p1.dto.LineupPlayerDTO;
import com.example.p1.dto.LineupResponseDTO;
import com.example.p1.repository.GameLineupRepository;
import com.example.p1.repository.GameScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional; // Optional 임포트

@Service
@RequiredArgsConstructor
@Transactional
public class GameLineupServiceImpl implements GameLineupService {

    private final GameLineupRepository gameLineupRepository;
    private final GameScheduleRepository gameScheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public LineupResponseDTO getLineupByGameId(Long gameId) {
        List<GameLineup> lineups = gameLineupRepository.findAllByGameIdWithPlayers(gameId);

        if (lineups.isEmpty()) {
            return LineupResponseDTO.builder().gameId(gameId).build();
        }

        LineupResponseDTO.LineupResponseDTOBuilder responseBuilder = LineupResponseDTO.builder()
                .gameId(gameId);

        GameLineupDTO homeLineupDTO = null;
        GameLineupDTO awayLineupDTO = null;

        for (GameLineup lineup : lineups) {
            GameLineupDTO lineupDTO = toDTO(lineup);

            if (lineup.getTeamType() == TeamType.HOME && homeLineupDTO == null) {
                homeLineupDTO = lineupDTO;
            } else if (lineup.getTeamType() == TeamType.AWAY && awayLineupDTO == null) {
                awayLineupDTO = lineupDTO;
            }
        }

        responseBuilder.homeLineup(homeLineupDTO);
        responseBuilder.awayLineup(awayLineupDTO);

        return responseBuilder.build();
    }

    @Override
    public GameLineupDTO createOrUpdateLineup(Long gameId, GameLineupDTO lineupDTO) {

        GameSchedule gameSchedule = gameScheduleRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("GameSchedule not found with ID: " + gameId));

        GameLineup gameLineup;

        if (lineupDTO.getId() != null) {
            gameLineup = gameLineupRepository.findById(lineupDTO.getId())
                    .orElseThrow(() -> new EntityNotFoundException("GameLineup not found with ID: " + lineupDTO.getId()));

            gameLineup.getPlayers().clear();
            gameLineup.setTeamType(lineupDTO.getTeamType());

        } else {
            gameLineup = GameLineup.builder()
                    .game(gameSchedule)
                    .teamType(lineupDTO.getTeamType())
                    .build();
        }

        if (lineupDTO.getPlayers() != null) {
            for (LineupPlayerDTO playerDTO : lineupDTO.getPlayers()) {
                LineupPlayer player = LineupPlayer.builder()
                        // LineupPlayerDTO의 orderNumber가 Integer이므로 null 체크 필요
                        .orderNumber(Optional.ofNullable(playerDTO.getOrderNumber()).orElse(0))
                        .playerName(playerDTO.getPlayerName())
                        .position(playerDTO.getPosition())
                        .playerRole(playerDTO.getPlayerRole())
                        .innings(playerDTO.getInnings())
                        .build();
                gameLineup.addPlayer(player);
            }
        }

        GameLineup savedLineup = gameLineupRepository.save(gameLineup);

        return toDTO(savedLineup);
    }

    @Override
    public void deleteLineupByGameId(Long gameId) {
        gameLineupRepository.deleteByGameId(gameId);
    }

    /**
     * GameLineup 엔티티를 GameLineupDTO로 변환합니다.
     * PlayerRole과 orderNumber를 기준으로 정렬합니다.
     */
    @Override
    public GameLineupDTO toDTO(GameLineup entity) {
        List<LineupPlayerDTO> playerDTOs = entity.getPlayers().stream()
                .map(this::toPlayerDTO)
                .sorted(Comparator
                        .comparing((LineupPlayerDTO player) -> {
                            if (player.getPlayerRole() == null) return 2;
                            return player.getPlayerRole() == PlayerRole.BATTER ? 0 : 1;
                        })
                        // LineupPlayerDTO의 orderNumber가 Integer이므로 null 체크 필요
                        .thenComparing(player -> Optional.ofNullable(player.getOrderNumber()).orElse(Integer.MAX_VALUE))
                        .thenComparing(LineupPlayerDTO::getPlayerName)
                )
                .collect(Collectors.toList());

        return GameLineupDTO.builder()
                .id(entity.getId())
                .gameId(entity.getGame().getId())
                .teamType(entity.getTeamType())
                .players(playerDTOs)
                .build();
    }

    /**
     * LineupPlayer 엔티티를 LineupPlayerDTO로 변환합니다.
     */
    private LineupPlayerDTO toPlayerDTO(LineupPlayer entity) {
        return LineupPlayerDTO.builder()
                .id(entity.getId())
                .playerName(entity.getPlayerName())
                .orderNumber(entity.getOrderNumber())
                .position(entity.getPosition())
                .playerRole(entity.getPlayerRole())
                .innings(entity.getInnings())
                .build();
    }
}