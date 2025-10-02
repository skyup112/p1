package com.example.p1.service;

import com.example.p1.domain.Member;
import com.example.p1.dto.RegisterRequestDTO;
import com.example.p1.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerNewUser(RegisterRequestDTO registerRequest) {
        // 중복 확인
        if (memberRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("사용자 ID가 이미 존재합니다!");
        }
        if (memberRepository.findByNickname(registerRequest.getNickname()).isPresent()) {
            throw new IllegalArgumentException("닉네임이 이미 존재합니다!");
        }
        if (memberRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이메일이 이미 존재합니다!");
        }
        if (memberRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("휴대전화번호가 이미 존재합니다!");
        }

        // 새 멤버 생성 및 저장
        Member newMember = Member.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .name(registerRequest.getName())
                .nickname(registerRequest.getNickname())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .role(Member.Role.USER) // 기본 역할은 USER
                .banned(false)
                .bannedUntil(null)
                .build();

        memberRepository.save(newMember);
    }
}