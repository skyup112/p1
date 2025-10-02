//package com.example.p1.service;
//
//import com.example.p1.domain.TeamRanking;
//import com.example.p1.dto.TeamRankingDTO;
//
//import java.util.List;
//
///**
// * 팀 순위 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
// */
//public interface TeamRankingService {
//
//    /**
//     * 특정 시즌의 모든 팀 순위 정보를 조회합니다.
//     * @param seasonYear 조회할 시즌 연도
//     * @return 해당 시즌의 팀 순위 DTO 목록 (순위 오름차순 정렬)
//     */
//    List<TeamRankingDTO> getAllTeamRankings(int seasonYear);
//
//    /**
//     * 특정 ID의 팀 순위 정보를 조회합니다.
//     * @param id 조회할 팀 순위의 ID
//     * @return 조회된 팀 순위 DTO
//     */
//    TeamRankingDTO getTeamRankingById(Long id);
//
//    /**
//     * 새로운 팀 순위 정보를 생성합니다.
//     * (주로 내부 로직이나 관리자용으로 사용될 수 있습니다.)
//     * @param teamRankingDTO 생성할 팀 순위 정보 DTO
//     * @return 생성된 팀 순위 DTO
//     */
//    TeamRankingDTO createTeamRanking(TeamRankingDTO teamRankingDTO);
//
//    /**
//     * 기존 팀 순위 정보를 업데이트합니다.
//     * (주로 내부 로직이나 관리자용으로 사용될 수 있습니다.)
//     * @param id 업데이트할 팀 순위의 ID
//     * @param teamRankingDTO 업데이트할 팀 순위 정보 DTO
//     * @return 업데이트된 팀 순위 DTO
//     */
//    TeamRankingDTO updateTeamRanking(Long id, TeamRankingDTO teamRankingDTO);
//
//    /**
//     * 특정 ID의 팀 순위 정보를 삭제합니다.
//     * @param id 삭제할 팀 순위의 ID
//     */
//    void deleteTeamRanking(Long id);
//
//    /**
//     * 특정 시즌의 경기 결과를 바탕으로 팀 순위를 계산하고 저장합니다.
//     * 이 메서드는 주기적으로 호출되거나, 경기 결과가 업데이트될 때 호출될 수 있습니다.
//     * @param seasonYear 순위를 계산할 시즌 연도
//     * @return 계산 및 저장된 팀 순위 DTO 목록
//     */
//    List<TeamRankingDTO> calculateAndSaveRankingsForSeason(int seasonYear);
//
//    /**
//     * TeamRanking 엔티티를 TeamRankingDTO로 변환합니다.
//     * @param teamRanking 변환할 TeamRanking 엔티티
//     * @return 변환된 TeamRankingDTO
//     */
//    TeamRankingDTO toDTO(TeamRanking teamRanking);
//}

// src/main/java/com/example/p1/service/TeamRankingService.java
package com.example.p1.service;

import com.example.p1.domain.TeamRanking;
import com.example.p1.dto.TeamRankingDTO;

import java.io.IOException; // IOException 임포트 추가
import java.util.List;

/**
 * 팀 순위 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
 */
public interface TeamRankingService {

    /**
     * 특정 시즌의 모든 팀 순위 정보를 조회합니다.
     * @param seasonYear 조회할 시즌 연도
     * @return 해당 시즌의 팀 순위 DTO 목록 (순위 오름차순 정렬)
     */
    List<TeamRankingDTO> getAllTeamRankings(int seasonYear);

    /**
     * 특정 ID의 팀 순위 정보를 조회합니다.
     * @param id 조회할 팀 순위의 ID
     * @return 조회된 팀 순위 DTO
     */
    TeamRankingDTO getTeamRankingById(Long id);

    /**
     * 새로운 팀 순위 정보를 생성합니다.
     * (주로 내부 로직이나 관리자용으로 사용될 수 있습니다.)
     * @param teamRankingDTO 생성할 팀 순위 정보 DTO
     * @return 생성된 팀 순위 DTO
     */
    TeamRankingDTO createTeamRanking(TeamRankingDTO teamRankingDTO);

    /**
     * 기존 팀 순위 정보를 업데이트합니다.
     * (주로 내부 로직이나 관리자용으로 사용될 수 있습니다.)
     * 프론트에서 게임차를 수동으로 수정할 수 있게 했으므로,
     * 게임차 값을 포함한 모든 필드를 DTO에서 넘어온 값으로 업데이트합니다.
     * @param id 업데이트할 팀 순위의 ID
     * @param teamRankingDTO 업데이트할 팀 순위 정보 DTO
     * @return 업데이트된 팀 순위 DTO
     */
    TeamRankingDTO updateTeamRanking(Long id, TeamRankingDTO teamRankingDTO);

    /**
     * 특정 ID의 팀 순위 정보를 삭제합니다.
     * @param id 삭제할 팀 순위의 ID
     */
    void deleteTeamRanking(Long id);

    /**
     * 특정 시즌의 경기 결과를 바탕으로 팀 순위를 계산하고 저장합니다.
     * 이 메서드는 주기적으로 호출되거나, 경기 결과가 업데이트될 때 호출될 수 있습니다.
     * @param seasonYear 순위를 계산할 시즌 연도
     * @return 계산 및 저장된 팀 순위 DTO 목록
     */
    List<TeamRankingDTO> calculateAndSaveRankingsForSeason(int seasonYear);

    /**
     * TeamRanking 엔티티를 TeamRankingDTO로 변환합니다.
     * @param teamRanking 변환할 TeamRanking 엔티티
     * @return 변환된 TeamRankingDTO
     */
    TeamRankingDTO toDTO(TeamRanking teamRanking);

    /**
     * KBO 웹사이트에서 실시간 순위 데이터를 크롤링하여 팀 순위를 업데이트하거나 생성합니다.
     * 이 메서드는 크롤링된 승,패,무,승률,순위,게임차를 그대로 DB에 반영합니다.
     * @param seasonYear 크롤링된 순위를 적용할 시즌 연도
     * @return 업데이트되거나 생성된 TeamRankingDTO 목록
     * @throws IOException 크롤링 실패 시 발생
     */
    List<TeamRankingDTO> updateRankingsFromCrawl(int seasonYear) throws IOException;
}
