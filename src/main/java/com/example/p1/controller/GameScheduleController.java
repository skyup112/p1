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
}