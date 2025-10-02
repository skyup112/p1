package com.example.p1.service;

import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.GameStatus;
import com.example.p1.domain.Team;
import com.example.p1.dto.CommentDTO;
import com.example.p1.dto.GameLineupDTO;
import com.example.p1.dto.GameScheduleDTO;
import com.example.p1.dto.TeamDTO;
import com.example.p1.repository.CommentRepository;
import com.example.p1.repository.GameScheduleRepository;
import com.example.p1.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Optional 임포트 추가 (findByGameKey 사용 시 필요)
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameScheduleServiceImpl implements GameScheduleService {

    private static final Logger log = LoggerFactory.getLogger(GameScheduleServiceImpl.class);

    private final GameScheduleRepository gameScheduleRepository;
    private final CommentRepository commentRepository; // 사용되지 않는 경우 제거 고려
    private final GameLineupService gameLineupService;
    private final TeamRepository teamRepository;
    private final CommentService commentService;
    private final KboGameCrawlerService kboGameCrawlerService;

    // KBO 웹사이트의 약식 팀명(크롤러가 반환하는 이름)과 DB에 저장된 정식 팀명 간의 매핑
    private static final Map<String, String> KBO_TEAM_NAME_MAPPING;

    static {
        KBO_TEAM_NAME_MAPPING = new HashMap<>();
        KBO_TEAM_NAME_MAPPING.put("한화", "한화 이글스");
        KBO_TEAM_NAME_MAPPING.put("LG", "LG 트윈스");
        KBO_TEAM_NAME_MAPPING.put("롯데", "롯데 자이언츠");
        KBO_TEAM_NAME_MAPPING.put("KIA", "KIA 타이거즈");
        KBO_TEAM_NAME_MAPPING.put("SSG", "SSG 랜더스");
        KBO_TEAM_NAME_MAPPING.put("KT", "KT 위즈");
        KBO_TEAM_NAME_MAPPING.put("삼성", "삼성 라이온즈");
        KBO_TEAM_NAME_MAPPING.put("NC", "NC 다이노스");
        KBO_TEAM_NAME_MAPPING.put("두산", "두산 베어스");
        KBO_TEAM_NAME_MAPPING.put("키움", "키움 히어로즈");
    }

    /**
     * 경기 상태를 결정하는 헬퍼 메서드.
     * 점수 유무, 경기 시간, 취소 여부를 바탕으로 경기 상태를 결정합니다.
     * @param game 상태를 확인할 GameSchedule 엔티티
     * @return 결정된 GameStatus (CANCELED, FINISHED, SCHEDULED)
     */
    private GameStatus determineGameStatus(GameSchedule game) {
        if (game.getStatus() == GameStatus.CANCELED) {
            return GameStatus.CANCELED;
        }
        // 점수가 0이 아니면 (즉, 득점이 발생했으면) 종료된 경기로 간주
        if (game.getHomeScore() != 0 || game.getAwayScore() != 0) {
            return GameStatus.FINISHED;
        }
        // 경기 시간이 현재 시간보다 이전이면 종료된 경기로 간주
        if (game.getGameDate() != null && game.getGameDate().isBefore(LocalDateTime.now())) {
            return GameStatus.FINISHED;
        }
        return GameStatus.SCHEDULED;
    }

    @Override
    public GameScheduleDTO createGame(GameScheduleDTO gameDTO) {
        log.info("Creating new game with DTO: {}", gameDTO);
        Team homeTeam = teamRepository.findById(gameDTO.getHomeTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("홈 팀을 찾을 수 없습니다: " + gameDTO.getHomeTeam().getId()));

        Team opponentTeam = teamRepository.findById(gameDTO.getOpponentTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("상대 팀을 찾을 수 없습니다: " + gameDTO.getOpponentTeam().getId()));

        // gameKey는 createGame에서는 일반적으로 설정하지 않습니다.
        // 이 메소드는 관리자가 수동으로 게임을 추가하는 경우에 사용될 수 있습니다.
        // 크롤링된 게임은 updateGameSchedulesFromCrawl에서 gameKey를 설정합니다.
        // 만약 createGame에서도 gameKey가 필수라면, DTO에 gameKey를 추가하고 여기서 설정해야 합니다.
        // 현재는 nullable=false이므로, 이 메소드를 사용하는 경우 gameKey를 DTO에서 받아 설정해야 합니다.
        // 아니면, 이 createGame 메소드가 gameKey가 없는 수동 생성용이라면 DB gameKey 컬럼을 nullable=true로 변경해야 합니다.
        // 여기서는 DTO에 gameKey가 없다고 가정하고, Builder에 강제로 임시 값을 넣습니다.
        // 실제 운영에서는 이 부분을 사용 목적에 맞게 조정해야 합니다.
        String tempGameKey = "MANUAL_GAME_" + System.currentTimeMillis(); // 임시 gameKey
        if (gameDTO.getGameKey() != null) { // DTO에 gameKey가 있다면 사용
            tempGameKey = gameDTO.getGameKey();
        }


        GameSchedule game = GameSchedule.builder()
                .gameDate(gameDTO.getGameDate())
                .gameKey(tempGameKey) // 여기에 gameKey를 설정해야 합니다!
                .homeTeam(homeTeam)
                .opponentTeam(opponentTeam)
                .location(gameDTO.getLocation())
                .homeScore(gameDTO.getHomeScore())
                .awayScore(gameDTO.getAwayScore())
                .status(gameDTO.getStatus() != null ? GameStatus.valueOf(gameDTO.getStatus()) : GameStatus.SCHEDULED)
                .build();

        GameSchedule savedGame = gameScheduleRepository.save(game);
        log.info("Game created successfully with ID: {}", savedGame.getId());
        return toDTO(savedGame);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameScheduleDTO> getAllGames() {
        log.info("Fetching all game schedules.");
        return gameScheduleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GameScheduleDTO getGame(Long id) {
        log.info("Fetching game schedule with ID: {}", id);
        GameSchedule game = gameScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경기 없음: " + id));
        return toDTO(game);
    }

    @Override
    public GameScheduleDTO updateGame(Long id, GameScheduleDTO updatedGameDTO) {
        log.info("Updating game schedule with ID: {}", id);
        GameSchedule game = gameScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경기 없음: " + id));

        if (updatedGameDTO.getHomeTeam() != null && updatedGameDTO.getHomeTeam().getId() != null) {
            Team homeTeam = teamRepository.findById(updatedGameDTO.getHomeTeam().getId())
                    .orElseThrow(() -> new IllegalArgumentException("업데이트할 홈 팀을 찾을 수 없습니다: " + updatedGameDTO.getHomeTeam().getId()));
            game.setHomeTeam(homeTeam);
        }
        if (updatedGameDTO.getOpponentTeam() != null && updatedGameDTO.getOpponentTeam().getId() != null) {
            Team opponentTeam = teamRepository.findById(updatedGameDTO.getOpponentTeam().getId())
                    .orElseThrow(() -> new IllegalArgumentException("업데이트할 상대 팀을 찾을 수 없습니다: " + updatedGameDTO.getOpponentTeam().getId()));
            game.setOpponentTeam(opponentTeam);
        }

        if (updatedGameDTO.getGameDate() != null) game.setGameDate(updatedGameDTO.getGameDate());
        if (updatedGameDTO.getLocation() != null) game.setLocation(updatedGameDTO.getLocation());
        game.setHomeScore(updatedGameDTO.getHomeScore());
        game.setAwayScore(updatedGameDTO.getAwayScore());
        // gameKey는 보통 업데이트하지 않습니다. 고유 식별자이기 때문입니다.
        // if (updatedGameDTO.getGameKey() != null) game.setGameKey(updatedGameDTO.getGameKey());

        if (updatedGameDTO.getStatus() != null) {
            game.setStatus(GameStatus.valueOf(updatedGameDTO.getStatus()));
        } else {
            game.setStatus(determineGameStatus(game));
        }

        GameSchedule savedGame = gameScheduleRepository.save(game);
        log.info("Game schedule with ID: {} updated successfully.", savedGame.getId());
        return toDTO(savedGame);
    }

    @Override
    public void deleteGame(Long id) {
        log.info("Deleting game schedule with ID: {}", id);
        if (!gameScheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 경기 없음: " + id);
        }
        gameScheduleRepository.deleteById(id);
        log.info("Game schedule with ID: {} deleted successfully.", id);
    }

    // --- DTO <-> Entity 변환 메서드 (서비스 내부에 위치) ---

    private TeamDTO toTeamDTO(Team team) {
        if (team == null) {
            return null;
        }
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .logoUrl(team.getLogoUrl())
                .build();
    }

    private GameScheduleDTO toDTO(GameSchedule game) {
        if (game == null) {
            return null;
        }

        List<GameLineupDTO> lineupDTOs = new ArrayList<>();
        if (game.getLineups() != null && !game.getLineups().isEmpty()) {
            lineupDTOs = game.getLineups().stream()
                    .map(gameLineupService::toDTO)
                    .collect(Collectors.toList());
        } else {
            log.debug("No lineups found for game ID: {}", game.getId());
        }


        List<CommentDTO> commentDTOs = new ArrayList<>();
        if (game.getComments() != null && !game.getComments().isEmpty()) {
            commentDTOs = game.getComments().stream()
                    .map(commentService::toDTO)
                    .collect(Collectors.toList());
        } else {
            log.debug("No comments found for game ID: {}", game.getId());
        }

        return GameScheduleDTO.builder()
                .id(game.getId())
                .gameKey(game.getGameKey()) // gameKey를 DTO에 포함
                .gameDate(game.getGameDate())
                .homeTeam(toTeamDTO(game.getHomeTeam()))
                .opponentTeam(toTeamDTO(game.getOpponentTeam()))
                .location(game.getLocation())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .status(game.getStatus() != null ? game.getStatus().name() : null)
                .lineups(lineupDTOs)
                .comments(commentDTOs)
                .build();
    }

    // --- 크롤링 및 DB 업데이트 로직 ---
    @Override
    public List<GameScheduleDTO> updateGameSchedulesFromCrawl(int seasonYear, int month) throws IOException {
        log.info("Starting updateGameSchedulesFromCrawl for year: {}, month: {}", seasonYear, month);

        List<Map<String, String>> crawledGames = kboGameCrawlerService.crawlKboSchedule(seasonYear, month);
        log.info("Crawled {} games from KBO website.", crawledGames.size());

        List<GameSchedule> schedulesToSave = new ArrayList<>();

        Map<String, Team> dbTeamFullNameMap = teamRepository.findAll().stream()
                .collect(Collectors.toMap(Team::getName, team -> team, (existing, replacement) -> existing));
        log.info("Loaded {} teams from DB.", dbTeamFullNameMap.size());

        LocalDateTime startOfMonth = LocalDateTime.of(seasonYear, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59).minusNanos(1);

        // 기존에는 gameDate와 팀 이름으로 식별했지만, 이제 gameKey를 사용하여 식별합니다.
        List<GameSchedule> existingSchedules = gameScheduleRepository
                .findByGameDateBetween(startOfMonth, endOfMonth); // 여전히 날짜 범위로 가져오되, 아래에서 gameKey로 맵핑

        Map<String, GameSchedule> existingScheduleMapByGameKey = existingSchedules.stream()
                .filter(gs -> gs.getGameKey() != null && !gs.getGameKey().isEmpty()) // gameKey가 있는 경우만 맵에 추가
                .collect(Collectors.toMap(GameSchedule::getGameKey, game -> game,
                        (existing, replacement) -> existing // gameKey 중복 시 기존 것 유지 (발생하지 않아야 함)
                ));
        log.info("Existing schedule map by gameKey size: {}", existingScheduleMapByGameKey.size());


        // Formatter for the expected full date-time pattern (e.g., "2025-07-31T18:00:00")
        DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        // Formatter for date-only pattern (e.g., "2025-07-31") - Used as fallback if time is missing
        DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        for (Map<String, String> gameData : crawledGames) {
            log.debug("Processing crawled game data: {}", gameData);

            String crawledGameKey = gameData.get("gameKey"); // 크롤러에서 얻은 gameKey
            String homeTeamShortName = gameData.get("homeTeamShortName");
            String awayTeamShortName = gameData.get("awayTeamShortName");
            String gameDateTimeStr = gameData.get("gameDateTime");

            // gameKey는 null이 아니어야 합니다. 크롤러에서 이미 확보했다고 가정합니다.
            if (crawledGameKey == null || crawledGameKey.isEmpty()) {
                log.warn("Warning: Crawled game data missing 'gameKey'. Skipping this game.");
                continue;
            }

            String homeTeamFullName = KBO_TEAM_NAME_MAPPING.get(homeTeamShortName);
            String awayTeamFullName = KBO_TEAM_NAME_MAPPING.get(awayTeamShortName);

            if (homeTeamFullName == null || awayTeamFullName == null) {
                log.warn("Warning: 크롤링된 팀명 매핑 실패. 홈팀 약식: {}, 원정팀 약식: {}. 해당 경기 건너뜀.", homeTeamShortName, awayTeamShortName);
                continue;
            }

            Team homeTeam = dbTeamFullNameMap.get(homeTeamFullName);
            Team opponentTeam = dbTeamFullNameMap.get(awayTeamFullName);

            if (homeTeam == null || opponentTeam == null) {
                log.warn("Warning: 데이터베이스에서 팀을 찾을 수 없습니다. 홈팀 정식: {}, 원정팀 정식: {}. 해당 경기 건너뜀.", homeTeamFullName, awayTeamFullName);
                continue;
            }

            LocalDateTime gameDate = null;
            try {
                gameDate = LocalDateTime.parse(gameDateTimeStr, fullDateTimeFormatter);
            } catch (DateTimeParseException e) {
                try {
                    gameDate = LocalDateTime.of(LocalDate.parse(gameDateTimeStr, dateOnlyFormatter), LocalTime.MIDNIGHT);
                    log.warn("Parsed date-only string '{}' to LocalDateTime with MIDNIGHT time for game.", gameDateTimeStr);
                } catch (DateTimeParseException ex) {
                    log.error("Error parsing game date time '{}': {}. 해당 경기 건너뜀.", gameDateTimeStr, ex.getMessage(), ex);
                    continue;
                }
            } catch (Exception e) {
                log.error("An unexpected error occurred parsing game date time '{}': {}. 해당 경기 건너뜀.", gameDateTimeStr, e.getMessage(), e);
                continue;
            }


            // gameKey를 사용하여 기존 일정을 찾습니다.
            GameSchedule gameSchedule = existingScheduleMapByGameKey.get(crawledGameKey);

            if (gameSchedule == null) {
                log.info("New game schedule detected for gameKey: {}", crawledGameKey);
                gameSchedule = GameSchedule.builder()
                        .gameKey(crawledGameKey) // <-- **여기에서 gameKey를 설정합니다!**
                        .gameDate(gameDate)
                        .location(gameData.get("stadium"))
                        .homeTeam(homeTeam)
                        .opponentTeam(opponentTeam)
                        .homeScore(0) // 초기 점수는 0으로 설정
                        .awayScore(0)
                        .status(GameStatus.SCHEDULED) // 초기 상태는 SCHEDULED
                        .build();
            } else {
                log.info("Existing game schedule found for gameKey: {}. Updating...", crawledGameKey);
                // 기존 gameSchedule의 필드를 업데이트합니다.
                // gameKey, gameDate, homeTeam, opponentTeam 등은 변하지 않는다고 가정하고 점수와 상태만 업데이트
            }

            // 점수 및 상태 업데이트 (신규/기존 모두 해당)
            int parsedHomeScore = 0;
            int parsedAwayScore = 0;
            try {
                parsedHomeScore = Integer.parseInt(gameData.getOrDefault("homeScore", "0"));
                parsedAwayScore = Integer.parseInt(gameData.getOrDefault("awayScore", "0"));
            } catch (NumberFormatException e) {
                log.warn("Error parsing scores for game {}: {}. Scores will be set to 0.", crawledGameKey, e.getMessage());
            }
            gameSchedule.setHomeScore(parsedHomeScore);
            gameSchedule.setAwayScore(parsedAwayScore);

            String statusStr = gameData.getOrDefault("status", "SCHEDULED");
            try {
                gameSchedule.setStatus(GameStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                log.warn("Error parsing game status '{}' for game {}: {}. Status will be SCHEDULED.", statusStr, crawledGameKey, e.getMessage());
                gameSchedule.setStatus(GameStatus.SCHEDULED); // 유효하지 않은 상태 문자열의 경우 기본값 설정
            }
            gameSchedule.setLocation(gameData.get("stadium")); // 경기장 정보도 업데이트

            schedulesToSave.add(gameSchedule);
            log.debug("Prepared to save/update game: ID={}, GameKey={}, Date={}, Home={}, Opponent={}, Status={}, Score={}:{}",
                    gameSchedule.getId() != null ? gameSchedule.getId() : "NEW",
                    gameSchedule.getGameKey(), // gameKey 로깅 추가
                    gameSchedule.getGameDate(),
                    gameSchedule.getHomeTeam().getName(),
                    gameSchedule.getOpponentTeam().getName(),
                    gameSchedule.getStatus(),
                    gameSchedule.getHomeScore(), gameSchedule.getAwayScore());
        }

        log.info("Attempting to save {} schedules.", schedulesToSave.size());
        List<GameSchedule> savedSchedules = gameScheduleRepository.saveAll(schedulesToSave);
        log.info("Successfully saved/updated {} schedules.", savedSchedules.size());

        return savedSchedules.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // gameKey를 기반으로 식별하는 것이 더 정확하므로, generateGameIdentifier는 이제 사용되지 않을 수 있습니다.
    // 하지만 gameKey가 없을 때의 fallback으로 남겨두거나, 로깅/디버깅 목적으로 유지할 수 있습니다.
    private String generateGameIdentifier(LocalDateTime gameDate, String homeTeamName, String opponentTeamName) {
        return gameDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "-" + homeTeamName + "-" + opponentTeamName;
    }
}