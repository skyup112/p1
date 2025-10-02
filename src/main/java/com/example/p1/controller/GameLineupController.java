package com.example.p1.controller;

import com.example.p1.dto.GameLineupDTO;
import com.example.p1.dto.LineupResponseDTO; // Import the new combined DTO
import com.example.p1.service.GameLineupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/{gameId}/lineup") // Nested resource for lineups
@RequiredArgsConstructor
public class GameLineupController {

    private final GameLineupService gameLineupService;

    /**
     * 특정 게임의 홈 팀과 원정 팀 라인업을 모두 조회합니다.
     * 프론트엔드에서 두 라인업을 동시에 표시하기 위해 사용됩니다.
     *
     * @param gameId 조회할 게임의 ID
     * @return 홈 팀 및 원정 팀 라인업 정보를 포함하는 LineupResponseDTO
     */
    @GetMapping
    public ResponseEntity<LineupResponseDTO> getGameLineup(@PathVariable Long gameId) {
        // 서비스 계층에서 LineupResponseDTO를 반환하도록 변경되었으므로, 컨트롤러도 이에 맞춰 변경
        LineupResponseDTO lineupResponse = gameLineupService.getLineupByGameId(gameId);
        // 라인업이 없어도 (null DTOs) 200 OK를 반환하여 프론트엔드에서 처리하도록 합니다.
        // 프론트엔드에서는 homeLineup 또는 awayLineup이 null인지 확인하여 "라인업 없음"을 표시할 수 있습니다.
        return ResponseEntity.ok(lineupResponse);
    }

    /**
     * 특정 게임의 홈 또는 원정 라인업을 생성하거나 업데이트합니다.
     * 요청 본문에 teamType (HOME 또는 AWAY)이 포함되어야 합니다.
     *
     * @param gameId 생성 또는 업데이트할 게임의 ID
     * @param lineupDTO 생성 또는 업데이트할 라인업 정보 (teamType 포함)
     * @return 저장되거나 업데이트된 GameLineupDTO
     */
    @PutMapping
    public ResponseEntity<GameLineupDTO> createOrUpdateGameLineup(
            @PathVariable Long gameId,
            @RequestBody GameLineupDTO lineupDTO) {
        // lineupDTO에 teamType 필드가 포함되어야 합니다.
        // 서비스 계층에서 이 teamType을 사용하여 해당 팀의 라인업을 식별하고 처리합니다.
        GameLineupDTO savedLineup = gameLineupService.createOrUpdateLineup(gameId, lineupDTO);
        return new ResponseEntity<>(savedLineup, HttpStatus.OK);
    }

    /**
     * 특정 게임에 연결된 모든 라인업 (홈 및 원정)을 삭제합니다.
     *
     * @param gameId 삭제할 게임의 ID
     * @return 응답 본문 없음 (204 No Content)
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteGameLineup(@PathVariable Long gameId) {
        gameLineupService.deleteLineupByGameId(gameId);
        return ResponseEntity.noContent().build();
    }
}