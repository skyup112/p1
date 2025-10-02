package com.example.p1.controller;

import com.example.p1.dto.MemberDTO;
import com.example.p1.dto.MemberUpdateRequestDTO;
import com.example.p1.dto.PasswordChangeRequestDTO;
import com.example.p1.dto.MemberDeleteRequestDTO;
import com.example.p1.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException; // NoSuchElementException 임포트 추가

@RestController
@RequestMapping("/api/members") // 일반 사용자용 경로
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // 인증된 사용자만 접근 가능
public class MemberController {

    private final MemberService memberService;

    /**
     * 현재 로그인된 사용자 자신의 정보를 조회합니다.
     * @param userDetails Spring Security에서 제공하는 현재 사용자 정보
     * @return MemberDTO 형태의 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<MemberDTO> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        // ⭐ 이 부분을 수정합니다! Optional<MemberDTO>를 처리하도록 변경 ⭐
        try {
            MemberDTO memberInfo = memberService.getMemberByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new NoSuchElementException("Member not found for username: " + userDetails.getUsername()));
            return ResponseEntity.ok(memberInfo);
        } catch (NoSuchElementException e) {
            // 회원을 찾을 수 없을 때 404 Not Found 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * 현재 로그인된 사용자 자신의 정보를 수정합니다.
     * @param userDetails Spring Security에서 제공하는 현재 사용자 정보
     * @param updateRequest 수정할 정보가 담긴 DTO
     * @return 수정된 MemberDTO 형태의 사용자 정보
     */
    @PutMapping("/me")
    public ResponseEntity<MemberDTO> updateMyInfo(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestBody MemberUpdateRequestDTO updateRequest) {
        try {
            MemberDTO updatedMember = memberService.updateMyInfo(userDetails.getUsername(), updateRequest);
            return ResponseEntity.ok(updatedMember);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 충돌 발생 시 409
        }
    }

    /**
     * 현재 로그인된 사용자 자신의 비밀번호를 변경합니다.
     * @param userDetails Spring Security에서 제공하는 현재 사용자 정보
     * @param passwordChangeRequest 현재 비밀번호와 새 비밀번호가 담긴 DTO
     * @return 성공 시 200 OK
     */
    @PutMapping("/me/password")
    public ResponseEntity<String> changeMyPassword(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestBody PasswordChangeRequestDTO passwordChangeRequest) {
        try {
            memberService.changePassword(userDetails.getUsername(), passwordChangeRequest);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 현재 로그인된 사용자 자신의 계정을 탈퇴합니다.
     * @param userDetails Spring Security에서 제공하는 현재 사용자 정보
     * @param deleteRequest 비밀번호 재확인이 담긴 DTO
     * @return 성공 시 204 No Content
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestBody MemberDeleteRequestDTO deleteRequest) {
        try {
            memberService.deleteMyAccount(userDetails.getUsername(), deleteRequest);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); // 관리자 탈퇴 시도 등
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage()); // 비밀번호 불일치
        }
    }
}
