package com.example.p1.controller;

import com.example.p1.dto.LineupResponseDTO;
import com.example.p1.service.GameLineupService;
import com.example.p1.service.LineupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/lineups")
@RequiredArgsConstructor
public class LineupController {

    private final LineupService lineupService;
    private final GameLineupService gameLineupService;

    /**
     * 특정 경기의 라인업 정보를 데이터베이스에서 조회하여 반환합니다.
     * URL: GET /api/lineups/{gameId}
     * * @param gameId 조회할 경기의 ID
     * @return 홈팀/원정팀 라인업 정보를 포함하는 DTO
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<LineupResponseDTO> getLineupsByGameId(@PathVariable Long gameId) {
        // GameLineupService를 사용하여 데이터베이스에서 라인업 조회 및 DTO 변환
        LineupResponseDTO responseDTO = gameLineupService.getLineupByGameId(gameId);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 특정 경기의 라인업을 네이버 스포츠에서 크롤링하고 데이터베이스에 저장합니다.
     * 이 엔드포인트는 사용자가 UI에서 "라인업 크롤링" 버튼을 클릭했을 때 호출됩니다.
     * URL: POST /api/lineups/{gameId}/crawl
     * * @param gameId 크롤링할 경기의 ID
     * @return 크롤링 결과 메시지
     */
    @PostMapping("/{gameId}/crawl")
    public ResponseEntity<String> crawlLineupsForGame(@PathVariable Long gameId) {
        try {
            // LineupService를 사용하여 크롤링 및 저장 로직 실행
            lineupService.crawlAndSaveLineups(gameId);
            return ResponseEntity.ok("Lineups successfully crawled and saved for game ID: " + gameId);
        } catch (IllegalArgumentException e) {
            // 예: gameId를 찾을 수 없거나 팀 이름이 잘못된 경우
            System.err.println("Crawling error (IllegalArgument): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (IOException e) {
            // 예: Selenium WebDriver 오류 또는 네트워크 문제
            System.err.println("Crawling failed (IOException): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Crawling failed: " + e.getMessage());
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            System.err.println("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}