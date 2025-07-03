package com.example.p1.service;

import com.example.p1.dto.MemberDTO;
import com.example.p1.dto.MemberUpdateRequestDTO;
import com.example.p1.dto.PasswordChangeRequestDTO;
import com.example.p1.dto.MemberDeleteRequestDTO; // 추가
import java.time.LocalDateTime;
import java.util.List;

public interface MemberService {
    List<MemberDTO> getAllMembers();
    MemberDTO getMemberById(Long id);
    MemberDTO getMemberByUsername(String username);

    // 사용자 본인이 정보 수정 (AdminMemberController의 updateMember와는 다름)
    MemberDTO updateMyInfo(String username, MemberUpdateRequestDTO updateRequest);

    // 비밀번호 변경
    void changePassword(String username, PasswordChangeRequestDTO passwordChangeRequest);

    // 회원 탈퇴
    void deleteMyAccount(String username, MemberDeleteRequestDTO deleteRequest);

    // 관리자 기능 (기존과 동일)
    MemberDTO updateMember(Long id, MemberDTO memberDTO); // 관리자용 (예: 역할 변경 등)
    void deleteMember(Long id); // 관리자용
    MemberDTO banMemberPermanently(String username);
    MemberDTO banMemberTemporarily(String username, LocalDateTime until);
    MemberDTO unbanMember(String username);
}