package com.example.p1.controller;

import com.example.p1.dto.GameScheduleDTO;
import com.example.p1.service.GameScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameScheduleController {

    private final GameScheduleService gameScheduleService;

    @PostMapping
    public ResponseEntity<GameScheduleDTO> createGame(@RequestBody GameScheduleDTO gameDTO) {
        GameScheduleDTO createdGame = gameScheduleService.createGame(gameDTO);
        return new ResponseEntity<>(createdGame, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GameScheduleDTO>> getGames() {
        List<GameScheduleDTO> games = gameScheduleService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameScheduleDTO> getGameById(@PathVariable Long id) {
        GameScheduleDTO game = gameScheduleService.getGame(id);
        return ResponseEntity.ok(game);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameScheduleDTO> updateGame(@PathVariable Long id, @RequestBody GameScheduleDTO updatedGameDTO) {
        GameScheduleDTO updatedGame = gameScheduleService.updateGame(id, updatedGameDTO);
        return ResponseEntity.ok(updatedGame);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGame(@PathVariable Long id) {
        gameScheduleService.deleteGame(id);
        return ResponseEntity.ok("게임 삭제 완료");
    }
    @PostMapping("/crawl-and-update")
    public ResponseEntity<String> crawlAndUpdateGameSchedules(
            @RequestParam int seasonYear,
            @RequestParam int month) {
        try {
            // Service 메서드 호출 이름을 인터페이스에 정의된 이름과 일치시킵니다.
            gameScheduleService.updateGameSchedulesFromCrawl(seasonYear, month); // <-- 이 부분 수정
            return ResponseEntity.ok("경기 일정 크롤링 및 업데이트 성공");
        } catch (Exception e) {
            // 실제 서비스 로직에서 발생하는 예외를 더 구체적으로 처리할 수 있습니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("경기 일정 크롤링 중 오류 발생: " + e.getMessage());
        }
    }
}