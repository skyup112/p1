package com.example.p1.controller;

import com.example.p1.dto.MemberDTO;
import com.example.p1.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor // Lombok이 final 필드에 대한 생성자를 자동으로 생성합니다.
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final MemberService memberService;
    /**
     * 모든 회원 목록을 조회합니다.
     * @return 회원 DTO 목록
     */
    @GetMapping
    public ResponseEntity<List<MemberDTO>> getAllMembers() {
        List<MemberDTO> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    /**
     * 특정 회원의 정보를 수정합니다.
     * @param memberId 수정할 회원의 ID
     * @param memberDTO 수정할 회원 정보
     * @return 수정된 회원 DTO
     */
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable Long memberId, @RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO updatedMember = memberService.updateMember(memberId, memberDTO);
            return ResponseEntity.ok(updatedMember);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    /**
     * 특정 회원을 삭제합니다.
     * @param memberId 삭제할 회원의 ID
     * @return 응답 없음 (204 No Content)
     */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        try {
            memberService.deleteMember(memberId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * 회원을 일시적으로 정지시킵니다.
     * @param request Map containing "username" and "bannedUntil" (ISO String)
     * @return 성공 메시지
     */
    @PostMapping("/ban/temp")
    public ResponseEntity<String> banMemberTemporarily(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String bannedUntilString = request.get("bannedUntil");

        if (username == null || bannedUntilString == null) {
            return ResponseEntity.badRequest().body("Username and bannedUntil date are required.");
        }

        // --- 진단용 로그 추가 ---
        System.out.println("Received bannedUntilString for ban in AdminMemberController: " + bannedUntilString);
        // --- 진단용 로그 끝 ---

        try {
            Instant instant = Instant.parse(bannedUntilString);
            LocalDateTime bannedUntil = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

            memberService.banMemberTemporarily(username, bannedUntil);
            return ResponseEntity.ok("회원이 일시적으로 정지되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("유효하지 않은 정지 기한 날짜 형식입니다: " + bannedUntilString + " (오류: " + e.getMessage() + ")");
        }
    }

    /**
     * 회원을 영구 정지시킵니다.
     * @param request Map containing "username"
     * @return 성공 메시지
     */
    @PostMapping("/ban/permanent")
    public ResponseEntity<String> banMemberPermanently(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        try {
            memberService.banMemberPermanently(username);
            return ResponseEntity.ok("회원이 영구 정지되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 회원 정지를 해제합니다.
     * @param request Map containing "username"
     * @return 성공 메시지
     */
    @PostMapping("/unban")
    public ResponseEntity<String> unbanMember(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        try {
            memberService.unbanMember(username);
            return ResponseEntity.ok("회원 정지가 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
