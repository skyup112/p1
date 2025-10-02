package com.example.p1.service;

import com.example.p1.domain.Member;
import com.example.p1.dto.MemberDTO;
import com.example.p1.dto.MemberUpdateRequestDTO;
import com.example.p1.dto.PasswordChangeRequestDTO;
import com.example.p1.dto.MemberDeleteRequestDTO;
import com.example.p1.dto.RegisterRequestDTO;
import com.example.p1.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // Optional 임포트 추가
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // Member 엔티티를 MemberDTO로 변환하는 헬퍼 메서드
    private MemberDTO toDTO(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .username(member.getUsername())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .role(member.getRole().name())
                .banned(member.isBanned())
                .bannedUntil(member.getBannedUntil())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDTO getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + id));
        return toDTO(member);
    }

    @Override
    @Transactional(readOnly = true)
    // ⭐ 이 부분을 수정합니다! ⭐
    public Optional<MemberDTO> getMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .map(this::toDTO); // Optional<Member>를 Optional<MemberDTO>로 변환
    }

    @Override
    public void register(RegisterRequestDTO registerRequest) {
        // 아이디, 닉네임, 이메일, 전화번호 중복 확인
        if (memberRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.findByNickname(registerRequest.getNickname()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (memberRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 휴대전화번호입니다.");
        }

        Member newMember = Member.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword())) // 비밀번호 인코딩
                .name(registerRequest.getName())
                .nickname(registerRequest.getNickname())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .role(Member.Role.USER) // 기본 역할은 USER
                .banned(false)
                .build();
        memberRepository.save(newMember);
    }

    @Override
    public MemberDTO updateMyInfo(String username, MemberUpdateRequestDTO updateRequest) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));

        if (updateRequest.getNickname() != null && !updateRequest.getNickname().isEmpty() &&
                !member.getNickname().equals(updateRequest.getNickname()) &&
                memberRepository.findByNickname(updateRequest.getNickname()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다!");
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty() &&
                !member.getEmail().equals(updateRequest.getEmail()) &&
                memberRepository.findByEmail(updateRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다!");
        }
        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().isEmpty() &&
                !member.getPhoneNumber().equals(updateRequest.getPhoneNumber()) &&
                memberRepository.findByPhoneNumber(updateRequest.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 휴대전화번호입니다!");
        }

        if (updateRequest.getName() != null && !updateRequest.getName().isEmpty()) {
            member.setName(updateRequest.getName());
        }
        if (updateRequest.getNickname() != null && !updateRequest.getNickname().isEmpty()) {
            member.setNickname(updateRequest.getNickname());
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty()) {
            member.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().isEmpty()) {
            member.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        Member updatedMember = memberRepository.save(member);
        return toDTO(updatedMember);
    }

    @Override
    public void changePassword(String username, PasswordChangeRequestDTO passwordChangeRequest) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));

        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        member.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        memberRepository.save(member);
    }

    @Override
    public void deleteMyAccount(String username, MemberDeleteRequestDTO deleteRequest) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));

        if (!passwordEncoder.matches(deleteRequest.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다. 탈퇴할 수 없습니다.");
        }

        if (member.getRole() == Member.Role.ADMIN) {
            throw new SecurityException("관리자 계정은 탈퇴할 수 없습니다.");
        }

        memberRepository.delete(member);
    }

    @Override
    public MemberDTO updateMember(Long id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + id));

        if (memberDTO.getRole() != null && !memberDTO.getRole().isEmpty()) {
            try {
                member.setRole(Member.Role.valueOf(memberDTO.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 역할입니다: " + memberDTO.getRole());
            }
        }

        Member updatedMember = memberRepository.save(member);
        return toDTO(updatedMember);
    }

    @Override
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다: " + id));

        if (member.getRole() == Member.Role.ADMIN) {
            throw new SecurityException("관리자 계정은 삭제할 수 없습니다.");
        }
        memberRepository.delete(member);
    }

    @Override
    public MemberDTO banMemberPermanently(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));
        if (member.getRole() == Member.Role.ADMIN) {
            throw new SecurityException("관리자 계정은 정지할 수 없습니다.");
        }
        member.setBanned(true);
        member.setBannedUntil(null);
        return toDTO(memberRepository.save(member));
    }

    @Override
    public MemberDTO banMemberTemporarily(String username, LocalDateTime until) {
        System.out.println("Entering banMemberTemporarily in MemberServiceImpl for username: " + username + " until: " + until);

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));
        if (member.getRole() == Member.Role.ADMIN) {
            throw new SecurityException("관리자 계정은 정지할 수 없습니다.");
        }
        member.setBanned(true);
        member.setBannedUntil(until);
        return toDTO(memberRepository.save(member));
    }

    @Override
    public MemberDTO unbanMember(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다: " + username));
        member.setBanned(false);
        member.setBannedUntil(null);
        return toDTO(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameExists(String username) {
        return memberRepository.findByUsername(username).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }
}