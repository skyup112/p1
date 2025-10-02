package com.example.p1.repository;

import com.example.p1.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // Optional

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByNickname(String nickname); // 닉네임 중복 확인
    Optional<Member> findByEmail(String email);       // 이메일 중복 확인
    Optional<Member> findByPhoneNumber(String phoneNumber); // 휴대전화번호 중복 확인//
}