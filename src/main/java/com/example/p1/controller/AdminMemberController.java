package com.example.p1.controller;

import com.example.p1.dto.MemberDTO;
import com.example.p1.domain.Member; // domain 패키지의 Member 엔티티 임포트
import com.example.p1.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // Optional 임포트
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/members") // 관리자 전용 경로
@PreAuthorize("hasRole('ADMIN')") // 이 컨트롤러의 모든 메서드는 ADMIN 역할만 접근 가능
public class MemberController {

    private final MemberRepository memberRepository;

    public MemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // Member 엔티티를 MemberDTO로 변환하는 헬퍼 메서드
    private MemberDTO convertToDto(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .username(member.getUsername())
                .role(member.getRole().name()) // Enum을 String으로 변환
                .banned(member.isBanned())
                .bannedUntil(member.getBannedUntil())
                .build();
    }

    @GetMapping
    public ResponseEntity<List<MemberDTO>> getAllMembers() {
        List<MemberDTO> members = memberRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{username}")
    public ResponseEntity<MemberDTO> getMemberByUsername(@PathVariable String username) {
        return memberRepository.findByUsername(username)
                .map(member -> ResponseEntity.ok(convertToDto(member)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 회원 정보 업데이트 (관리자가 역할 변경 등에 사용)
    // 이 예시에서는 DTO에서 받은 정보로만 업데이트하며, 비밀번호 변경은 별도 API 권장
    @PutMapping("/{id}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable Long id, @RequestBody MemberDTO memberDTO) {
        Optional<Member> existingMemberOpt = memberRepository.findById(id);
        if (existingMemberOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Member existingMember = existingMemberOpt.get();

        // 관리자 역할은 API로 변경하지 않도록 제한 (DB에서만 관리)
        // if (memberDTO.getRole() != null) {
        //     existingMember.setRole(Member.Role.valueOf(memberDTO.getRole().toUpperCase()));
        // }

        // 밴 상태는 ban/unban API를 통해 관리되므로 여기서 직접 수정하지 않음
        // existingMember.setBanned(memberDTO.isBanned());
        // existingMember.setBannedUntil(memberDTO.getBannedUntil());

        // 다른 필드 (예: username)는 변경하지 않도록 주의
        // 필요에 따라 업데이트 가능한 필드를 DTO에 명시하고 해당 필드만 업데이트

        Member updatedMember = memberRepository.save(existingMember);
        return ResponseEntity.ok(convertToDto(updatedMember));
    }


    @PostMapping("/{username}/ban/permanent")
    public ResponseEntity<MemberDTO> banMemberPermanently(@PathVariable String username) {
        return memberRepository.findByUsername(username)
                .map(member -> {
                    // 관리자 계정은 밴할 수 없음
                    if (member.getRole() == Member.Role.ADMIN) {
                        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
                    }
                    member.setBanned(true);
                    member.setBannedUntil(null); // 영구 밴
                    memberRepository.save(member);
                    return ResponseEntity.ok(convertToDto(member));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{username}/ban/temporary")
    public ResponseEntity<MemberDTO> banMemberTemporarily(@PathVariable String username, @RequestParam LocalDateTime until) {
        return memberRepository.findByUsername(username)
                .map(member -> {
                    // 관리자 계정은 밴할 수 없음
                    if (member.getRole() == Member.Role.ADMIN) {
                        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
                    }
                    member.setBanned(true); // 일시 정지도 banned를 true로 설정
                    member.setBannedUntil(until);
                    memberRepository.save(member);
                    return ResponseEntity.ok(convertToDto(member));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{username}/unban")
    public ResponseEntity<MemberDTO> unbanMember(@PathVariable String username) {
        return memberRepository.findByUsername(username)
                .map(member -> {
                    member.setBanned(false);
                    member.setBannedUntil(null);
                    memberRepository.save(member);
                    return ResponseEntity.ok(convertToDto(member));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        Optional<Member> memberOpt = memberRepository.findById(id);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 관리자 계정은 삭제하지 못하도록 제한
        if (memberOpt.get().getRole() == Member.Role.ADMIN) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        memberRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}