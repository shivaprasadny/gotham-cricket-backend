package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String fullName;
    // null when the member has disabled showEmail
    private String email;
    private Role role;
    private UserStatus status;
    private String nickname;

    // Contact — null when the member has disabled showPhone
    private String countryCode;
    private String phone;

    // Frontend needs this flag to decide whether to show the WhatsApp button
    // even when phone is visible (user might have phone but no WhatsApp).
    private boolean showWhatsApp;

    // Cricket profile
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;

    // Profile image (S3 pre-signed download URL — may be null if no image set)
    private String profileImageUrl;
    private LocalDateTime profileImageUpdatedAt;
}
