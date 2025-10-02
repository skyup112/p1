package com.example.p1.service;

import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.GameLineup;
import com.example.p1.domain.LineupPlayer;
import com.example.p1.domain.TeamType;
import com.example.p1.domain.PlayerRole;
import com.example.p1.repository.GameLineupRepository;
import com.example.p1.repository.GameScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LineupServiceImpl implements LineupService {

    private static final Logger log = LoggerFactory.getLogger(LineupServiceImpl.class);

    private final KboGameCrawlerService kboGameCrawlerService;
    private final GameScheduleRepository gameScheduleRepository;
    private final GameLineupRepository gameLineupRepository;

    // KboGameCrawlerService와 동일한 매핑을 사용하여 일관성 유지 (선수 크롤링에서는 직접 사용되지 않음)
    private static final Map<String, String> KBO_FULL_TO_DISPLAY_NAME_MAP = new HashMap<>();

    static {
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("롯데 자이언츠", "롯데");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("SSG 랜더스", "SSG");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("두산 베어스", "두산");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("키움 히어로즈", "키움");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("LG 트윈스", "LG");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("KT 위즈", "KT");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("NC 다이노스", "NC");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("삼성 라이온즈", "삼성");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("한화 이글스", "한화");
        KBO_FULL_TO_DISPLAY_NAME_MAP.put("KIA 타이거즈", "KIA");
    }

    @Override
    public void crawlAndSaveLineups(Long gameScheduleId) throws IOException {

        GameSchedule gameSchedule = gameScheduleRepository.findById(gameScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("GameSchedule not found with ID: " + gameScheduleId));

        log.info("Attempting to crawl and save players for GameSchedule ID: {}", gameScheduleId);

        // --- 기존 라인업 삭제 로직 ---
        if (!gameSchedule.getLineups().isEmpty()) {
            log.info("Clearing existing lineups for GameSchedule ID: {}", gameScheduleId);
            gameSchedule.getLineups().forEach(lineup -> lineup.setGame(null));
            gameSchedule.getLineups().clear();
        }

        // gameSchedule에서 gameKey를 가져옵니다.
        // GameSchedule 엔티티에 gameKey 필드가 있어야 하며, KboGameCrawlerService.crawlKboSchedule에서 이 필드를 채워야 합니다.
        String gameKey = gameSchedule.getGameKey(); // GameSchedule 엔티티에 gameKey 필드가 있다고 가정

        if (gameKey == null || gameKey.isEmpty()) {
            log.warn("GameKey is missing for GameSchedule ID: {}. Cannot crawl lineups. Skipping.", gameScheduleId);
            return;
        }

        // 크롤링에 필요한 팀 전체 이름을 가져옵니다.
        String homeTeamFullname = gameSchedule.getHomeTeam().getName();
        String awayTeamFullname = gameSchedule.getOpponentTeam().getName();

        log.info("Crawling lineups for gameKey: {}, Home: {}, Away: {}", gameKey, homeTeamFullname, awayTeamFullname);

        // gameKey를 첫 번째 인자로 전달하도록 수정
        List<Map<String, Object>> scrapedPlayersData = kboGameCrawlerService.crawlGamePlayersForGame(
                gameKey, homeTeamFullname, awayTeamFullname
        );

        if (scrapedPlayersData.isEmpty()) {
            log.warn("No player data was crawled for game ID: {}. This might be due to the game not being played yet, or no detailed records being available.", gameScheduleId);
            gameScheduleRepository.save(gameSchedule); // 라인업이 없어도 GameSchedule은 저장하여 상태를 업데이트할 수 있도록
            return;
        }

        Map<String, GameLineup> currentProcessingLineups = new HashMap<>();

        for (Map<String, Object> playerData : scrapedPlayersData) {
            String teamName = (String) playerData.get("teamName");
            String teamTypeStr = (String) playerData.get("teamType");
            String playerRoleStr = (String) playerData.get("playerRole");

            TeamType teamType = TeamType.valueOf(teamTypeStr);
            PlayerRole playerRole = PlayerRole.valueOf(playerRoleStr);

            String lineupKey = teamName + "_" + playerRole.name();

            GameLineup gameLineup = currentProcessingLineups.computeIfAbsent(lineupKey, k -> {
                GameLineup newGameLineup = GameLineup.builder()
                        .game(gameSchedule)
                        .teamType(teamType)
                        .build();
                gameSchedule.getLineups().add(newGameLineup);
                return newGameLineup;
            });

            LineupPlayer player = LineupPlayer.builder()
                    .playerName((String) playerData.get("playerName"))
                    .playerRole(playerRole)
                    .gameLineup(gameLineup)
                    .build();

            if (playerRole == PlayerRole.BATTER) {
                if (playerData.containsKey("orderNumber") && playerData.get("orderNumber") != null) {
                    try {
                        player.setOrderNumber(Integer.parseInt((String) playerData.get("orderNumber")));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid order number format for batter {}: {}. Defaulting to 0.", playerData.get("playerName"), playerData.get("orderNumber"));
                        player.setOrderNumber(0);
                    }
                } else {
                    player.setOrderNumber(0);
                }
                player.setPosition((String) playerData.get("position"));
                player.setInnings(null);
            } else if (playerRole == PlayerRole.PITCHER) {
                player.setOrderNumber(0);
                player.setPosition((String) playerData.get("position"));
                player.setInnings((String) playerData.get("innings"));
            }

            gameLineup.addPlayer(player);
        }

        gameScheduleRepository.save(gameSchedule);
        log.info("Successfully crawled and saved {} player entries for game ID: {}", scrapedPlayersData.size(), gameScheduleId);
    }
}