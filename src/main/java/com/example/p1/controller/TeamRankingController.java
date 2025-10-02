//package com.example.p1.controller;
//
//import com.example.p1.dto.TeamRankingDTO;
//import com.example.p1.service.TeamRankingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * 팀 순위 관련 API 요청을 처리하는 컨트롤러.
// */
//@RestController
//@RequestMapping("/api/rankings") // 팀 순위 API의 기본 경로
//@RequiredArgsConstructor
//public class TeamRankingController {
//
//    private final TeamRankingService teamRankingService;
//
//    /**
//     * 특정 시즌의 모든 팀 순위 정보를 조회합니다.
//     * @param seasonYear 조회할 시즌 연도 (쿼리 파라미터)
//     * @return 해당 시즌의 팀 순위 DTO 목록 (순위 오름차순 정렬)
//     */
//    @GetMapping
//    public ResponseEntity<List<TeamRankingDTO>> getAllTeamRankings(@RequestParam int seasonYear) {
//        List<TeamRankingDTO> rankings = teamRankingService.getAllTeamRankings(seasonYear);
//        return ResponseEntity.ok(rankings);
//    }
//
//    /**
//     * 특정 ID의 팀 순위 정보를 조회합니다.
//     * @param id 조회할 팀 순위의 ID
//     * @return 조회된 팀 순위 DTO
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<TeamRankingDTO> getTeamRankingById(@PathVariable Long id) {
//        try {
//            TeamRankingDTO ranking = teamRankingService.getTeamRankingById(id);
//            return ResponseEntity.ok(ranking);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    /**
//     * 특정 시즌의 경기 결과를 바탕으로 팀 순위를 계산하고 저장합니다.
//     * 이 엔드포인트는 관리자만 접근할 수 있습니다.
//     * @param seasonYear 순위를 계산할 시즌 연도
//     * @return 계산 및 저장된 팀 순위 DTO 목록
//     */
//    @PostMapping("/calculate")
//    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
//    public ResponseEntity<List<TeamRankingDTO>> calculateAndSaveRankings(@RequestParam int seasonYear) {
//        try {
//            List<TeamRankingDTO> updatedRankings = teamRankingService.calculateAndSaveRankingsForSeason(seasonYear);
//            return ResponseEntity.ok(updatedRankings);
//        } catch (IllegalStateException e) {
//            // 예를 들어, 등록된 팀이 없거나 경기 정보가 부족할 때 발생
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//        } catch (Exception e) {
//            // 기타 예상치 못한 오류
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    /**
//     * 새로운 팀 순위 정보를 생성합니다. (관리자용)
//     * @param teamRankingDTO 생성할 팀 순위 정보 DTO
//     * @return 생성된 팀 순위 DTO
//     */
//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
//    public ResponseEntity<TeamRankingDTO> createTeamRanking(@RequestBody TeamRankingDTO teamRankingDTO) {
//        try {
//            TeamRankingDTO createdRanking = teamRankingService.createTeamRanking(teamRankingDTO);
//            return ResponseEntity.status(HttpStatus.CREATED).body(createdRanking);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 중복 등 데이터 유효성 문제
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    /**
//     * 기존 팀 순위 정보를 업데이트합니다. (관리자용)
//     * @param id 업데이트할 팀 순위의 ID
//     * @param teamRankingDTO 업데이트할 팀 순위 정보 DTO
//     * @return 업데이트된 팀 순위 DTO
//     */
//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
//    public ResponseEntity<TeamRankingDTO> updateTeamRanking(@PathVariable Long id, @RequestBody TeamRankingDTO teamRankingDTO) {
//        try {
//            TeamRankingDTO updatedRanking = teamRankingService.updateTeamRanking(id, teamRankingDTO);
//            return ResponseEntity.ok(updatedRanking);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 해당 ID의 순위 정보 없음
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    /**
//     * 특정 ID의 팀 순위 정보를 삭제합니다. (관리자용)
//     * @param id 삭제할 팀 순위의 ID
//     * @return 성공 시 204 No Content
//     */
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
//    public ResponseEntity<Void> deleteTeamRanking(@PathVariable Long id) {
//        try {
//            teamRankingService.deleteTeamRanking(id);
//            return ResponseEntity.noContent().build();
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 해당 ID의 순위 정보 없음
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//}
// src/main/java/com/example/p1/controller/TeamRankingController.java
package com.example.p1.controller;

import com.example.p1.dto.TeamRankingDTO;
import com.example.p1.service.TeamRankingService; // 인터페이스로 주입받음
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException; // IOException 임포트 추가
import java.util.List;

/**
 * 팀 순위 관련 API 요청을 처리하는 컨트롤러.
 */
@RestController
@RequestMapping("/api/rankings") // 팀 순위 API의 기본 경로
@RequiredArgsConstructor
public class TeamRankingController {

    private final TeamRankingService teamRankingService;

    /**
     * 특정 시즌의 모든 팀 순위 정보를 조회합니다.
     * @param seasonYear 조회할 시즌 연도 (쿼리 파라미터)
     * @return 해당 시즌의 팀 순위 DTO 목록 (순위 오름차순 정렬)
     */
    @GetMapping
    public ResponseEntity<List<TeamRankingDTO>> getAllTeamRankings(@RequestParam int seasonYear) {
        List<TeamRankingDTO> rankings = teamRankingService.getAllTeamRankings(seasonYear);
        return ResponseEntity.ok(rankings);
    }

    /**
     * 특정 ID의 팀 순위 정보를 조회합니다.
     * @param id 조회할 팀 순위의 ID
     * @return 조회된 팀 순위 DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamRankingDTO> getTeamRankingById(@PathVariable Long id) {
        try {
            TeamRankingDTO ranking = teamRankingService.getTeamRankingById(id);
            return ResponseEntity.ok(ranking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * 특정 시즌의 경기 결과를 바탕으로 팀 순위를 계산하고 저장합니다.
     * 이 엔드포인트는 관리자만 접근할 수 있습니다.
     * @param seasonYear 순위를 계산할 시즌 연도
     * @return 계산 및 저장된 팀 순위 DTO 목록
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<List<TeamRankingDTO>> calculateAndSaveRankings(@RequestParam int seasonYear) {
        try {
            List<TeamRankingDTO> updatedRankings = teamRankingService.calculateAndSaveRankingsForSeason(seasonYear);
            return ResponseEntity.ok(updatedRankings);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * KBO 웹사이트에서 실시간 순위 데이터를 크롤링하여 팀 순위를 업데이트합니다.
     * 이 엔드포인트는 관리자만 접근할 수 있습니다.
     * @param seasonYear 크롤링된 순위를 적용할 시즌 연도
     * @return 업데이트된 팀 순위 DTO 목록
     */
    @PostMapping("/crawl-and-update") // 새로운 엔드포인트
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<List<TeamRankingDTO>> crawlAndUpateRankings(@RequestParam int seasonYear) {
        try {
            // 이제 TeamRankingService 인터페이스에 메서드가 추가되었으므로 캐스팅 없이 직접 호출 가능
            List<TeamRankingDTO> updatedRankings = teamRankingService.updateRankingsFromCrawl(seasonYear);
            return ResponseEntity.ok(updatedRankings);
        } catch (IOException e) {
            System.err.println("Error during KBO ranking crawl: " + e.getMessage());
            // KBO 웹사이트 접속 또는 파싱 오류 시 502 Bad Gateway
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        } catch (Exception e) {
            System.err.println("Error updating rankings from crawl: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 새로운 팀 순위 정보를 생성합니다. (관리자용)
     * @param teamRankingDTO 생성할 팀 순위 정보 DTO
     * @return 생성된 팀 순위 DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<TeamRankingDTO> createTeamRanking(@RequestBody TeamRankingDTO teamRankingDTO) {
        try {
            TeamRankingDTO createdRanking = teamRankingService.createTeamRanking(teamRankingDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRanking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 중복 등 데이터 유효성 문제
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 기존 팀 순위 정보를 업데이트합니다. (관리자용)
     * 프론트에서 게임차를 수동으로 수정할 수 있게 했으므로, 해당 값도 업데이트됩니다.
     * @param id 업데이트할 팀 순위의 ID
     * @param teamRankingDTO 업데이트할 팀 순위 정보 DTO
     * @return 업데이트된 팀 순위 DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<TeamRankingDTO> updateTeamRanking(@PathVariable Long id, @RequestBody TeamRankingDTO teamRankingDTO) {
        try {
            TeamRankingDTO updatedRanking = teamRankingService.updateTeamRanking(id, teamRankingDTO);
            return ResponseEntity.ok(updatedRanking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 해당 ID의 순위 정보 없음
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 특정 ID의 팀 순위 정보를 삭제합니다. (관리자용)
     * @param id 삭제할 팀 순위의 ID
     * @return 성공 시 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<Void> deleteTeamRanking(@PathVariable Long id) {
        try {
            teamRankingService.deleteTeamRanking(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 해당 ID의 순위 정보 없음
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}