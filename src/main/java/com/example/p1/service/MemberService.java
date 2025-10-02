package com.example.p1.service;

import com.example.p1.dto.MemberDTO;
import com.example.p1.dto.MemberUpdateRequestDTO;
import com.example.p1.dto.PasswordChangeRequestDTO;
import com.example.p1.dto.MemberDeleteRequestDTO;
import com.example.p1.dto.RegisterRequestDTO;
import java.util.List;
import java.util.Optional; // Optional 임포트 추가

public interface MemberService {
    void register(RegisterRequestDTO registerRequest);
    boolean isUsernameExists(String username);
    boolean isEmailExists(String email);
    List<MemberDTO> getAllMembers();
    MemberDTO getMemberById(Long id);
    // ⭐ 이 부분을 수정합니다! ⭐
    Optional<MemberDTO> getMemberByUsername(String username);
    MemberDTO updateMyInfo(String username, MemberUpdateRequestDTO updateRequest);
    void changePassword(String username, PasswordChangeRequestDTO passwordChangeRequest);
    void deleteMyAccount(String username, MemberDeleteRequestDTO deleteRequest);
    MemberDTO updateMember(Long id, MemberDTO memberDTO);
    void deleteMember(Long id);
    MemberDTO banMemberPermanently(String username);
    MemberDTO banMemberTemporarily(String username, java.time.LocalDateTime until);
    MemberDTO unbanMember(String username);
}