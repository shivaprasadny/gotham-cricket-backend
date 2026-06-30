package com.gotham.cricket.service;

import com.gotham.cricket.dto.MemberResponse;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final S3Service s3Service;

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

        // If ALL three flags are false the user's profile was created before the privacy
        // feature existed and the DB DEFAULT FALSE was applied automatically — treat that
        // state as "never configured" and show everything.
        boolean neverConfigured = profile != null
                && Boolean.FALSE.equals(profile.getShowEmail())
                && Boolean.FALSE.equals(profile.getShowPhone())
                && Boolean.FALSE.equals(profile.getShowWhatsApp());

        boolean showEmail    = profile == null || neverConfigured || !Boolean.FALSE.equals(profile.getShowEmail());
        boolean showPhone    = profile == null || neverConfigured || !Boolean.FALSE.equals(profile.getShowPhone());
        boolean showWhatsApp = profile == null || neverConfigured || !Boolean.FALSE.equals(profile.getShowWhatsApp());

        String imageUrl = s3Service.generateDownloadUrl(user.getProfileImageKey(), 60);

        return new MemberResponse(
                user.getId(),
                user.getFullName(),
                showEmail ? user.getEmail() : null,
                user.getRole(),
                user.getStatus(),
                profile != null ? profile.getNickname()     : null,
                showPhone ? (profile != null ? profile.getCountryCode() : null) : null,
                showPhone ? (profile != null ? profile.getPhone()       : null) : null,
                showWhatsApp,
                profile != null ? profile.getBattingStyle() : null,
                profile != null ? profile.getBowlingStyle() : null,
                profile != null ? profile.getPlayerType()   : null,
                profile != null ? profile.getJerseyNumber() : null,
                imageUrl,
                user.getProfileImageUpdatedAt()
        );
    }
}
