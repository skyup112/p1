//package com.example.p1.service;
//
//import com.example.p1.domain.GameSchedule;
//import com.example.p1.domain.GameStatus;
//import com.example.p1.domain.Team;
//import com.example.p1.domain.TeamRanking;
//import com.example.p1.dto.TeamDTO;
//import com.example.p1.dto.TeamRankingDTO;
//import com.example.p1.repository.GameScheduleRepository;
//import com.example.p1.repository.TeamRankingRepository;
//import com.example.p1.repository.TeamRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 팀 순위 관련 비즈니스 로직을 구현하는 서비스 구현체.
// */
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class TeamRankingServiceImpl implements TeamRankingService {
//
//    private final TeamRankingRepository teamRankingRepository;
//    private final TeamRepository teamRepository;
//    private final GameScheduleRepository gameScheduleRepository;
//
//    /**
//     * TeamRanking 엔티티를 TeamRankingDTO로 변환하는 헬퍼 메서드.
//     * @param teamRanking 변환할 TeamRanking 엔티티
//     * @return 변환된 TeamRankingDTO
//     */
//    @Override
//    public TeamRankingDTO toDTO(TeamRanking teamRanking) {
//        if (teamRanking == null) {
//            return null;
//        }
//        return TeamRankingDTO.builder()
//                .id(teamRanking.getId())
//                .team(toTeamDTO(teamRanking.getTeam())) // Team 엔티티를 TeamDTO로 변환
//                .seasonYear(teamRanking.getSeasonYear())
//                .wins(teamRanking.getWins())
//                .losses(teamRanking.getLosses())
//                .draws(teamRanking.getDraws())
//                .winRate(teamRanking.getWinRate())
//                .currentRank(teamRanking.getCurrentRank())
//                .gamesBehind(teamRanking.getGamesBehind())
//                .createdAt(teamRanking.getCreatedAt())
//                .updatedAt(teamRanking.getUpdatedAt())
//                .build();
//    }
//
//    /**
//     * Team 엔티티를 TeamDTO로 변환하는 헬퍼 메서드.
//     * (다른 서비스에서 이미 정의되어 있을 수 있으나, 여기서는 독립적으로 정의)
//     * @param team 변환할 Team 엔티티
//     * @return 변환된 TeamDTO
//     */
//    private TeamDTO toTeamDTO(Team team) {
//        if (team == null) {
//            return null;
//        }
//        return TeamDTO.builder()
//                .id(team.getId())
//                .name(team.getName())
//                .logoUrl(team.getLogoUrl())
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TeamRankingDTO> getAllTeamRankings(int seasonYear) {
//        return teamRankingRepository.findBySeasonYearOrderByCurrentRankAsc(seasonYear).stream()
//                .map(this::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public TeamRankingDTO getTeamRankingById(Long id) {
//        TeamRanking teamRanking = teamRankingRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("팀 순위 정보를 찾을 수 없습니다: " + id));
//        return toDTO(teamRanking);
//    }
//
//    @Override
//    public TeamRankingDTO createTeamRanking(TeamRankingDTO teamRankingDTO) {
//        Team team = teamRepository.findById(teamRankingDTO.getTeam().getId())
//                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다: " + teamRankingDTO.getTeam().getId()));
//
//        if (teamRankingRepository.existsByTeamIdAndSeasonYear(team.getId(), teamRankingDTO.getSeasonYear())) {
//            throw new IllegalArgumentException("해당 팀의 해당 시즌 순위 정보가 이미 존재합니다.");
//        }
//
//        TeamRanking teamRanking = TeamRanking.builder()
//                .team(team)
//                .seasonYear(teamRankingDTO.getSeasonYear())
//                .wins(teamRankingDTO.getWins())
//                .losses(teamRankingDTO.getLosses())
//                .draws(teamRankingDTO.getDraws())
//                .winRate(teamRankingDTO.getWinRate())
//                .currentRank(teamRankingDTO.getCurrentRank())
//                .gamesBehind(teamRankingDTO.getGamesBehind())
//                .build();
//
//        TeamRanking savedTeamRanking = teamRankingRepository.save(teamRanking);
//        return toDTO(savedTeamRanking);
//    }
//
//    @Override
//    public TeamRankingDTO updateTeamRanking(Long id, TeamRankingDTO updatedTeamRankingDTO) {
//        TeamRanking teamRanking = teamRankingRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("팀 순위 정보를 찾을 수 없습니다: " + id));
//
//        // Note: DTO에서 값이 0 (또는 0.0)이 넘어오면 해당 필드를 업데이트하지 않는 로직입니다.
//        // 예를 들어, 승수가 0인 경우나 게임차가 0인 경우에도 업데이트가 되지 않을 수 있습니다.
//        // 모든 0 값을 유효한 업데이트로 간주하려면 if 문을 제거하거나 null 체크로 변경해야 합니다.
//        // 현재는 DTO의 기본값(primitive type의 0)이 넘어올 때 의도치 않은 업데이트 방지를 가정합니다.
//        if (updatedTeamRankingDTO.getWins() != 0) teamRanking.setWins(updatedTeamRankingDTO.getWins());
//        if (updatedTeamRankingDTO.getLosses() != 0) teamRanking.setLosses(updatedTeamRankingDTO.getLosses());
//        if (updatedTeamRankingDTO.getDraws() != 0) teamRanking.setDraws(updatedTeamRankingDTO.getDraws());
//        if (updatedTeamRankingDTO.getWinRate() != 0.0) teamRanking.setWinRate(updatedTeamRankingDTO.getWinRate());
//        if (updatedTeamRankingDTO.getCurrentRank() != 0) teamRanking.setCurrentRank(updatedTeamRankingDTO.getCurrentRank());
//        if (updatedTeamRankingDTO.getGamesBehind() != 0.0) teamRanking.setGamesBehind(updatedTeamRankingDTO.getGamesBehind());
//
//        TeamRanking savedTeamRanking = teamRankingRepository.save(teamRanking);
//        return toDTO(savedTeamRanking);
//    }
//
//    @Override
//    public void deleteTeamRanking(Long id) {
//        if (!teamRankingRepository.existsById(id)) {
//            throw new IllegalArgumentException("해당 팀 순위 정보가 없습니다: " + id);
//        }
//        teamRankingRepository.deleteById(id);
//    }
//
//    @Override
//    public List<TeamRankingDTO> calculateAndSaveRankingsForSeason(int seasonYear) {
//        // 1. 해당 시즌의 모든 팀 가져오기
//        List<Team> allTeams = teamRepository.findAll();
//        if (allTeams.isEmpty()) {
//            throw new IllegalStateException("등록된 팀이 없습니다. 순위를 계산할 수 없습니다.");
//        }
//
//        // 2. 각 팀별 승, 패, 무승부 집계
//        Map<Long, Map<String, Integer>> teamStats = new HashMap<>(); // TeamId -> { "wins": X, "losses": Y, "draws": Z }
//
//        // 모든 팀을 맵에 초기화
//        for (Team team : allTeams) {
//            teamStats.put(team.getId(), new HashMap<>(Map.of("wins", 0, "losses", 0, "draws", 0)));
//        }
//
//        // 해당 시즌의 완료된 모든 경기 조회
//        // GameScheduleRepository에 findByGameDateBetweenAndStatus 쿼리가 있다면 더 효율적입니다.
//        // 여기서는 findAll 후 필터링합니다.
//        List<GameSchedule> gamesInSeason = gameScheduleRepository.findAll().stream()
//                .filter(game -> game.getGameDate().getYear() == seasonYear && game.getStatus() == GameStatus.FINISHED)
//                .collect(Collectors.toList());
//
//        for (GameSchedule game : gamesInSeason) {
//            Long homeTeamId = game.getHomeTeam().getId();
//            Long awayTeamId = game.getOpponentTeam().getId();
//
//            // 통계 맵에서 해당 팀의 스탯을 가져옵니다.
//            // Map::getOrDefault 대신 미리 모든 팀을 초기화했으므로, 바로 get 해도 됩니다.
//            Map<String, Integer> homeStats = teamStats.get(homeTeamId);
//            Map<String, Integer> awayStats = teamStats.get(awayTeamId);
//
//            if (game.getHomeScore() > game.getAwayScore()) {
//                homeStats.put("wins", homeStats.get("wins") + 1);
//                awayStats.put("losses", awayStats.get("losses") + 1);
//            } else if (game.getHomeScore() < game.getAwayScore()) {
//                homeStats.put("losses", homeStats.get("losses") + 1);
//                awayStats.put("wins", awayStats.get("wins") + 1);
//            } else { // 무승부
//                homeStats.put("draws", homeStats.get("draws") + 1);
//                awayStats.put("draws", awayStats.get("draws") + 1);
//            }
//        }
//
//        // 3. TeamRanking 엔티티 생성 또는 업데이트를 위한 준비
//        List<TeamRanking> currentRankings = teamRankingRepository.findBySeasonYearOrderByCurrentRankAsc(seasonYear);
//        Map<Long, TeamRanking> existingRankingsMap = currentRankings.stream()
//                .collect(Collectors.toMap(tr -> tr.getTeam().getId(), tr -> tr));
//
//        List<TeamRanking> updatedOrNewRankings = allTeams.stream()
//                .map(team -> {
//                    Map<String, Integer> stats = teamStats.get(team.getId());
//                    int wins = stats.get("wins");
//                    int losses = stats.get("losses");
//                    int draws = stats.get("draws");
//
//                    // 야구 방식 승률 계산: wins / (wins + losses). 무승부는 승률 계산에 포함하지 않음.
//                    int totalDecisionGames = wins + losses; // 승패가 결정된 게임 수
//                    double winRate = (totalDecisionGames > 0) ? (double) wins / totalDecisionGames : 0.0;
//
//                    TeamRanking teamRanking = existingRankingsMap.getOrDefault(team.getId(), TeamRanking.builder()
//                            .team(team)
//                            .seasonYear(seasonYear)
//                            .build());
//
//                    teamRanking.setWins(wins);
//                    teamRanking.setLosses(losses);
//                    teamRanking.setDraws(draws);
//                    teamRanking.setWinRate(winRate);
//                    // rank와 gamesBehind는 나중에 계산하여 설정
//
//                    return teamRanking;
//                })
//                .collect(Collectors.toList());
//
//        // 4. 순위 및 게임차 계산
//        // 순위 정렬 기준: 1. 승률 내림차순, 2. 승수 내림차순, 3. 패수 오름차순
//        updatedOrNewRankings.sort(Comparator
//                .comparing(TeamRanking::getWinRate).reversed()
//                .thenComparing(TeamRanking::getWins).reversed()
//                .thenComparing(TeamRanking::getLosses));
//
//        if (!updatedOrNewRankings.isEmpty()) {
//            TeamRanking firstPlaceTeam = updatedOrNewRankings.get(0);
//            double firstPlaceWins = firstPlaceTeam.getWins();
//            double firstPlaceLosses = firstPlaceTeam.getLosses();
//
//            for (int i = 0; i < updatedOrNewRankings.size(); i++) {
//                TeamRanking tr = updatedOrNewRankings.get(i);
//                tr.setCurrentRank(i + 1); // 현재 순위 설정
//
//                // 게임차 계산 (Games Behind): (1등 승수 - 본인 승수) + (본인 패수 - 1등 패수) / 2
//                // 1등 팀은 게임차가 0.0
//                double gamesBehind = (tr.getId().equals(firstPlaceTeam.getId())) ? 0.0 :
//                        ((firstPlaceWins - tr.getWins()) + (tr.getLosses() - firstPlaceLosses)) / 2.0;
//                tr.setGamesBehind(gamesBehind);
//            }
//        }
//
//        // 5. 데이터베이스에 저장 (업데이트 또는 새로 생성)
//        List<TeamRanking> savedRankings = teamRankingRepository.saveAll(updatedOrNewRankings);
//
//        return savedRankings.stream()
//                .map(this::toDTO)
//                .collect(Collectors.toList());
//    }
//}
// src/main/java/com/example/p1/service/TeamRankingServiceImpl.java
// src/main/java/com/example/p1/service/TeamRankingServiceImpl.java
// src/main/java/com/example/p1/service/TeamRankingServiceImpl.java
package com.example.p1.service;

import com.example.p1.domain.GameSchedule;
import com.example.p1.domain.GameStatus;
import com.example.p1.domain.Team;
import com.example.p1.domain.TeamRanking;
import com.example.p1.dto.TeamDTO;
import com.example.p1.dto.TeamRankingDTO;
import com.example.p1.repository.GameScheduleRepository;
import com.example.p1.repository.TeamRankingRepository;
import com.example.p1.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 팀 순위 관련 비즈니스 로직을 구현하는 서비스 구현체.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamRankingServiceImpl implements TeamRankingService {

    private final TeamRankingRepository teamRankingRepository;
    private final TeamRepository teamRepository;
    private final GameScheduleRepository gameScheduleRepository;
    private final KboCrawlerService kboCrawlerService; // Jsoup 크롤링 서비스 주입

    // KBO 웹사이트의 약식 팀명과 DB에 저장된 정식 팀명 간의 매핑
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
        // 필요한 경우 여기에 추가 팀 매핑을 넣으세요.
    }

    /**
     * TeamRanking 엔티티를 TeamRankingDTO로 변환하는 헬퍼 메서드.
     * @param teamRanking 변환할 TeamRanking 엔티티
     * @return 변환된 TeamRankingDTO
     */
    @Override
    public TeamRankingDTO toDTO(TeamRanking teamRanking) {
        if (teamRanking == null) {
            return null;
        }
        return TeamRankingDTO.builder()
                .id(teamRanking.getId())
                .team(toTeamDTO(teamRanking.getTeam())) // Team 엔티티를 TeamDTO로 변환
                .seasonYear(teamRanking.getSeasonYear())
                .wins(teamRanking.getWins())
                .losses(teamRanking.getLosses())
                .draws(teamRanking.getDraws())
                .winRate(teamRanking.getWinRate())
                .currentRank(teamRanking.getCurrentRank())
                .gamesBehind(teamRanking.getGamesBehind())
                .createdAt(teamRanking.getCreatedAt())
                .updatedAt(teamRanking.getUpdatedAt())
                .build();
    }

    /**
     * Team 엔티티를 TeamDTO로 변환하는 헬퍼 메서드.
     * (다른 서비스에서 이미 정의되어 있을 수 있으나, 여기서는 독립적으로 정의)
     * @param team 변환할 Team 엔티티
     * @return 변환된 TeamDTO
     */
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

    @Override
    @Transactional(readOnly = true)
    public List<TeamRankingDTO> getAllTeamRankings(int seasonYear) {
        return teamRankingRepository.findBySeasonYearOrderByCurrentRankAsc(seasonYear).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeamRankingDTO getTeamRankingById(Long id) {
        TeamRanking teamRanking = teamRankingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀 순위 정보를 찾을 수 없습니다: " + id));
        return toDTO(teamRanking);
    }

    @Override
    public TeamRankingDTO createTeamRanking(TeamRankingDTO teamRankingDTO) {
        Team team = teamRepository.findById(teamRankingDTO.getTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다: " + teamRankingDTO.getTeam().getId()));

        if (teamRankingRepository.existsByTeamIdAndSeasonYear(team.getId(), teamRankingDTO.getSeasonYear())) {
            throw new IllegalArgumentException("해당 팀의 해당 시즌 순위 정보가 이미 존재합니다.");
        }

        TeamRanking teamRanking = TeamRanking.builder()
                .team(team)
                .seasonYear(teamRankingDTO.getSeasonYear())
                .wins(teamRankingDTO.getWins())
                .losses(teamRankingDTO.getLosses())
                .draws(teamRankingDTO.getDraws())
                .winRate(teamRankingDTO.getWinRate())
                .currentRank(teamRankingDTO.getCurrentRank())
                .gamesBehind(teamRankingDTO.getGamesBehind())
                .build();

        TeamRanking savedTeamRanking = teamRankingRepository.save(teamRanking);
        return toDTO(savedTeamRanking);
    }

    @Override
    public TeamRankingDTO updateTeamRanking(Long id, TeamRankingDTO updatedTeamRankingDTO) {
        TeamRanking teamRanking = teamRankingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀 순위 정보를 찾을 수 없습니다: " + id));

        teamRanking.setWins(updatedTeamRankingDTO.getWins());
        teamRanking.setLosses(updatedTeamRankingDTO.getLosses());
        teamRanking.setDraws(updatedTeamRankingDTO.getDraws());
        teamRanking.setWinRate(updatedTeamRankingDTO.getWinRate());
        teamRanking.setCurrentRank(updatedTeamRankingDTO.getCurrentRank());
        teamRanking.setGamesBehind(updatedTeamRankingDTO.getGamesBehind()); // 게임차 수동 업데이트 허용

        TeamRanking savedTeamRanking = teamRankingRepository.save(teamRanking);
        return toDTO(savedTeamRanking);
    }

    @Override
    public void deleteTeamRanking(Long id) {
        if (!teamRankingRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 팀 순위 정보가 없습니다: " + id);
        }
        teamRankingRepository.deleteById(id);
    }

    @Override
    public List<TeamRankingDTO> calculateAndSaveRankingsForSeason(int seasonYear) {
        // 1. 해당 시즌의 모든 팀 가져오기
        List<Team> allTeams = teamRepository.findAll();
        if (allTeams.isEmpty()) {
            throw new IllegalStateException("등록된 팀이 없습니다. 순위를 계산할 수 없습니다.");
        }

        // 2. 각 팀별 승, 패, 무승부 집계
        Map<Long, Map<String, Integer>> teamStats = new HashMap<>(); // TeamId -> { "wins": X, "losses": Y, "draws": Z }

        // 모든 팀을 맵에 초기화
        for (Team team : allTeams) {
            teamStats.put(team.getId(), new HashMap<>(Map.of("wins", 0, "losses", 0, "draws", 0)));
        }

        // 해당 시즌의 완료된 모든 경기 조회
        List<GameSchedule> gamesInSeason = gameScheduleRepository.findAll().stream()
                .filter(game -> game.getGameDate().getYear() == seasonYear && game.getStatus() == GameStatus.FINISHED)
                .collect(Collectors.toList());

        for (GameSchedule game : gamesInSeason) {
            Long homeTeamId = game.getHomeTeam().getId();
            Long awayTeamId = game.getOpponentTeam().getId();

            Map<String, Integer> homeStats = teamStats.get(homeTeamId);
            Map<String, Integer> awayStats = teamStats.get(awayTeamId);

            if (homeStats == null || awayStats == null) {
                System.err.println("Warning: Game involves a team not found in repository. Skipping game: " + game.getId());
                continue;
            }

            if (game.getHomeScore() > game.getAwayScore()) {
                homeStats.put("wins", homeStats.get("wins") + 1);
                awayStats.put("losses", awayStats.get("losses") + 1);
            } else if (game.getHomeScore() < game.getAwayScore()) {
                homeStats.put("losses", homeStats.get("losses") + 1);
                awayStats.put("wins", awayStats.get("wins") + 1);
            } else { // 무승부
                homeStats.put("draws", homeStats.get("draws") + 1);
                awayStats.put("draws", awayStats.get("draws") + 1);
            }
        }

        // 3. TeamRanking 엔티티 생성 또는 업데이트를 위한 준비
        List<TeamRanking> currentRankings = teamRankingRepository.findBySeasonYearOrderByCurrentRankAsc(seasonYear);
        Map<Long, TeamRanking> existingRankingsMap = currentRankings.stream()
                .collect(Collectors.toMap(tr -> tr.getTeam().getId(), tr -> tr));

        List<TeamRanking> updatedOrNewRankings = allTeams.stream()
                .map(team -> {
                    Map<String, Integer> stats = teamStats.get(team.getId());
                    int wins = stats != null ? stats.get("wins") : 0;
                    int losses = stats != null ? stats.get("losses") : 0;
                    int draws = stats != null ? stats.get("draws") : 0;

                    int totalDecisionGames = wins + losses;
                    double winRate = (totalDecisionGames > 0) ? (double) wins / totalDecisionGames : 0.0;

                    TeamRanking teamRanking = existingRankingsMap.getOrDefault(team.getId(), TeamRanking.builder()
                            .team(team)
                            .seasonYear(seasonYear)
                            .build());

                    teamRanking.setWins(wins);
                    teamRanking.setLosses(losses);
                    teamRanking.setDraws(draws);
                    teamRanking.setWinRate(winRate);

                    return teamRanking;
                })
                .collect(Collectors.toList());

        // 4. 순위 및 게임차 계산
        updatedOrNewRankings.sort(Comparator
                .comparing(TeamRanking::getWinRate).reversed()
                .thenComparing(TeamRanking::getWins).reversed()
                .thenComparing(TeamRanking::getLosses));

        if (!updatedOrNewRankings.isEmpty()) {
            TeamRanking firstPlaceTeam = updatedOrNewRankings.get(0);
            double firstPlaceWins = firstPlaceTeam.getWins();
            double firstPlaceLosses = firstPlaceTeam.getLosses();

            for (int i = 0; i < updatedOrNewRankings.size(); i++) {
                TeamRanking tr = updatedOrNewRankings.get(i);
                tr.setCurrentRank(i + 1);

                double gamesBehind = (tr.getId() != null && tr.getId().equals(firstPlaceTeam.getId())) ? 0.0 :
                        ((firstPlaceWins - tr.getWins()) + (tr.getLosses() - firstPlaceLosses)) / 2.0;
                tr.setGamesBehind(gamesBehind);
            }
        }

        // 5. 데이터베이스에 저장 (업데이트 또는 새로 생성)
        List<TeamRanking> savedRankings = teamRankingRepository.saveAll(updatedOrNewRankings);

        return savedRankings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeamRankingDTO> updateRankingsFromCrawl(int seasonYear) throws IOException {
        List<Map<String, String>> crawledData = kboCrawlerService.crawlCurrentKboTeamRanks();
        List<TeamRanking> rankingsToSave = new ArrayList<>();

        List<Team> allTeams = teamRepository.findAll();
        // DB에 있는 팀 이름을 ID로 매핑하여 빠르게 찾을 수 있도록 준비
        Map<String, Team> dbTeamFullNameMap = allTeams.stream()
                .collect(Collectors.toMap(Team::getName, team -> team, (existing, replacement) -> existing));

        List<TeamRanking> existingRankings = teamRankingRepository.findBySeasonYearOrderByCurrentRankAsc(seasonYear);
        Map<Long, TeamRanking> existingRankingsMap = existingRankings.stream()
                .collect(Collectors.toMap(tr -> tr.getTeam().getId(), tr -> tr));

        for (Map<String, String> data : crawledData) {
            String crawledShortTeamName = data.get("teamName");
            // 크롤링된 짧은 팀 이름을 정식 팀 이름으로 변환
            String dbTeamFullName = KBO_TEAM_NAME_MAPPING.get(crawledShortTeamName);

            if (dbTeamFullName == null) {
                System.err.println("Warning: 크롤링된 팀명 '" + crawledShortTeamName + "'에 대한 정식 매핑을 찾을 수 없습니다. 매핑 테이블을 확인해주세요.");
                continue; // 매핑이 없으면 건너뜜
            }

            Team team = dbTeamFullNameMap.get(dbTeamFullName);

            if (team == null) {
                System.err.println("Warning: 팀 '" + dbTeamFullName + "'을(를) 데이터베이스에서 찾을 수 없어 크롤링 데이터를 처리할 수 없습니다. DB에 해당 팀을 먼저 등록해주세요.");
                continue; // 해당 팀이 DB에 없으면 건너뜜
            }

            // 기존 순위 정보가 있는지 확인
            TeamRanking teamRanking = existingRankingsMap.get(team.getId());

            if (teamRanking == null) {
                // 새로운 순위 정보 생성
                teamRanking = TeamRanking.builder()
                        .team(team)
                        .seasonYear(seasonYear)
                        .build();
            }

            // 크롤링된 데이터로 업데이트
            try {
                teamRanking.setWins(Integer.parseInt(data.get("wins")));
                teamRanking.setLosses(Integer.parseInt(data.get("losses")));
                teamRanking.setDraws(Integer.parseInt(data.get("draws")));
                teamRanking.setWinRate(Double.parseDouble(data.get("winRate")));
                teamRanking.setCurrentRank(Integer.parseInt(data.get("rank")));

                // 게임차 (Games Behind) 처리: KBO 웹사이트에서 가져온 값을 그대로 사용
                String gapStr = data.get("gamesBehind");
                if (gapStr != null && (gapStr.equals("-") || gapStr.equals("0.0"))) {
                    teamRanking.setGamesBehind(0.0);
                } else {
                    teamRanking.setGamesBehind(Double.parseDouble(gapStr));
                }

                rankingsToSave.add(teamRanking);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing number from crawled data for team " + dbTeamFullName + ": " + e.getMessage());
                // 숫자 변환 오류가 발생하면 해당 팀의 순위 정보는 건너뜜
            }
        }

        // 데이터베이스에 저장 (새로 생성되거나 업데이트됨)
        List<TeamRanking> savedRankings = teamRankingRepository.saveAll(rankingsToSave);
        return savedRankings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
