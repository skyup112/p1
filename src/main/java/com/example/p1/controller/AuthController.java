package com.example.p1.controller;

import com.example.p1.dto.RegisterRequestDTO;
import com.example.p1.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// MemberRepository를 주입받아 닉네임을 가져오기 위해 추가
import com.example.p1.repository.MemberRepository;
import com.example.p1.domain.Member; // Member 도메인 임포트

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MemberRepository memberRepository; // MemberRepository 주입

    /**
     * 회원가입 요청을 처리합니다.
     * @param registerRequest 회원가입 요청 데이터 (username, password, name, nickname, email, phoneNumber)
     * @return 성공 시 201 Created, 중복 사용자 이름 등 예외 발생 시 409 Conflict
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            authService.registerNewUser(registerRequest);
            return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    /**
     * 현재 인증된 사용자 정보를 반환합니다.
     * @return 인증된 사용자 정보 (username, role, nickname) 또는 401 Unauthorized
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {

            Map<String, Object> userInfo = new HashMap<>();
            String username = authentication.getName();
            userInfo.put("username", username);

            authentication.getAuthorities().forEach(authority -> {
                if (authority.getAuthority().startsWith("ROLE_")) {
                    userInfo.put("role", authority.getAuthority().substring(5));
                }
            });

            // --- NEW: Fetch and add nickname ---
            memberRepository.findByUsername(username).ifPresent(member -> {
                userInfo.put("nickname", member.getNickname());
            });
            // --- END NEW ---

            return ResponseEntity.ok(userInfo);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Authenticated");
    }
}
