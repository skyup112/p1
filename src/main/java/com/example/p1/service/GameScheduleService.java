package com.example.p1.service;

import com.example.p1.dto.GameScheduleDTO; // DTO 임포트

import java.io.IOException;
import java.util.List;

public interface GameScheduleService {
    GameScheduleDTO createGame(GameScheduleDTO gameDTO); // DTO 사용
    List<GameScheduleDTO> getAllGames(); // DTO 사용
    GameScheduleDTO getGame(Long id); // DTO 사용
    GameScheduleDTO updateGame(Long id, GameScheduleDTO updatedGameDTO); // DTO 사용
    void deleteGame(Long id);
    List<GameScheduleDTO> updateGameSchedulesFromCrawl(int seasonYear, int month) throws IOException;
}