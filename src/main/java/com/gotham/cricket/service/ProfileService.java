package com.gotham.cricket.service;

import com.gotham.cricket.dto.ProfileResponse;
import com.gotham.cricket.dto.ProfileUpdateRequest;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public ProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        MemberProfile profile = memberProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    MemberProfile newProfile = new MemberProfile();
                    newProfile.setUser(user);
                    // Explicitly set defaults so they are stored correctly in DB.
                    newProfile.setShowEmail(true);
                    newProfile.setShowPhone(true);
                    newProfile.setShowWhatsApp(true);
                    return memberProfileRepository.save(newProfile);
                });

        // If ALL three flags are false the row was created before the privacy feature
        // existed. Migrate it to explicit true so future reads are correct.
        if (Boolean.FALSE.equals(profile.getShowEmail())
                && Boolean.FALSE.equals(profile.getShowPhone())
                && Boolean.FALSE.equals(profile.getShowWhatsApp())) {
            profile.setShowEmail(true);
            profile.setShowPhone(true);
            profile.setShowWhatsApp(true);
            memberProfileRepository.save(profile);
        }

        boolean showEmail    = !Boolean.FALSE.equals(profile.getShowEmail());
        boolean showPhone    = !Boolean.FALSE.equals(profile.getShowPhone());
        boolean showWhatsApp = !Boolean.FALSE.equals(profile.getShowWhatsApp());

        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                profile.getId(),
                profile.getNickname(),
                profile.getCountryCode(),
                profile.getPhone(),
                profile.getBattingStyle(),
                profile.getBowlingStyle(),
                profile.getPlayerType(),
                profile.getJerseyNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getJoinedClubDate(),
                showEmail,
                showPhone,
                showWhatsApp
        );
    }

    public String updateMyProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        MemberProfile profile = memberProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    MemberProfile newProfile = new MemberProfile();
                    newProfile.setUser(user);
                    return memberProfileRepository.save(newProfile);
                });

        // Member profile fields
        profile.setNickname(request.getNickname());
        profile.setCountryCode(request.getCountryCode());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());

        // Privacy toggles (null = keep existing value)
        if (request.getShowEmail() != null) profile.setShowEmail(request.getShowEmail());
        if (request.getShowPhone() != null) profile.setShowPhone(request.getShowPhone());
        if (request.getShowWhatsApp() != null) profile.setShowWhatsApp(request.getShowWhatsApp());

        // User table fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)  user.setLastName(request.getLastName());
        user.setFullName((user.getFirstName() + " " + user.getLastName()).trim());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            user.setDateOfBirth(java.time.LocalDate.parse(request.getDateOfBirth()));
        }

        userRepository.save(user);
        memberProfileRepository.save(profile);

        return "Profile updated successfully";
    }
}
