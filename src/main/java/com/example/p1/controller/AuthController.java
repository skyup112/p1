package com.example.p1.controller;

import com.example.p1.dto.RegisterRequestDTO;
import com.example.p1.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Authentication 임포트 추가
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder 임포트 추가
import org.springframework.security.core.GrantedAuthority; // GrantedAuthority 임포트 추가
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors; // Collectors 임포트 추가

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            memberService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 성공적으로 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsernameExists(@RequestParam String username) {
        boolean exists = memberService.isUsernameExists(username);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        boolean exists = memberService.isEmailExists(email);
        return ResponseEntity.ok(exists);
    }


    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkSessionStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> responseBody = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            // 로그인된 사용자 정보 반환
            responseBody.put("isAuthenticated", true);
            responseBody.put("username", authentication.getName());

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5)) // "ROLE_ADMIN" -> "ADMIN"
                    .findFirst()
                    .orElse("USER"); // 기본 역할은 USER

            responseBody.put("role", role);


            memberService.getMemberByUsername(authentication.getName()).ifPresent(memberDTO -> {
                responseBody.put("nickname", memberDTO.getNickname());
            });

            return ResponseEntity.ok(responseBody);
        } else {

            responseBody.put("isAuthenticated", false);
            responseBody.put("message", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
