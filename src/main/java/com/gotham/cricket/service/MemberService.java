package com.gotham.cricket.service;

import com.gotham.cricket.dto.MemberResponse;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public List<MemberResponse> getAllApprovedMembers() {
        return userRepository.findByStatus(UserStatus.APPROVED)
                .stream()
                .map(this::mapToMemberResponse)
                .toList();
    }

    public MemberResponse getMemberById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return mapToMemberResponse(user);
    }

    private MemberResponse mapToMemberResponse(User user) {
        MemberProfile profile = memberProfileRepository.findByUser(user).orElse(null);

        return new MemberResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getBattingStyle() : null,
                profile != null ? profile.getBowlingStyle() : null,
                profile != null ? profile.getPlayerType() : null,
                profile != null ? profile.getJerseyNumber() : null
        );
    }
}