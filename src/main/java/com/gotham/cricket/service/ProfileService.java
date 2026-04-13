package com.gotham.cricket.service;

import com.gotham.cricket.dto.ProfileResponse;
import com.gotham.cricket.dto.ProfileUpdateRequest;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public ProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        MemberProfile profile = memberProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profile not found for user email: " + email));

        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                profile.getId(),
                profile.getNickname(),
                profile.getPhone(),
                profile.getBattingStyle(),
                profile.getBowlingStyle(),
                profile.getPlayerType(),
                profile.getJerseyNumber()
        );
    }

    public String updateMyProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        MemberProfile profile = memberProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profile not found for user email: " + email));

        profile.setNickname(request.getNickname());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());

        memberProfileRepository.save(profile);

        return "Profile updated successfully";
    }
}