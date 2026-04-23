package com.gotham.cricket.service;

import com.gotham.cricket.dto.LoginRequest;
import com.gotham.cricket.dto.LoginResponse;
import com.gotham.cricket.dto.RegisterRequest;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import com.gotham.cricket.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    // User table repository
    private final UserRepository userRepository;

    // Member profile table repository
    private final MemberProfileRepository memberProfileRepository;

    // Password encoder for secure password storage
    private final PasswordEncoder passwordEncoder;

    // JWT token generator
    private final JwtService jwtService;

    // Notification service for admin alerts
    private final NotificationService notificationService;

    /**
     * Register a new user account.
     *
     * Flow:
     * 1. Check duplicate email
     * 2. Create user with PENDING status
     * 3. Build fullName from firstName + lastName
     * 4. Save optional profile details
     * 5. Notify admins about new join request
     */
    public String register(RegisterRequest request) {

        // Prevent duplicate email registration
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Build full name safely from first + last name
        String fullName =
                ((request.getFirstName() != null ? request.getFirstName().trim() : "") + " " +
                        (request.getLastName() != null ? request.getLastName().trim() : ""))
                        .trim();

        // Create new user
        User user = new User();

        // New structured name fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Keep fullName for backward compatibility
        user.setFullName(fullName);

        // Auth fields
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Default new members to PLAYER + PENDING
        user.setRole(Role.PLAYER);
        user.setStatus(UserStatus.PENDING);

        // New profile info stored directly on User
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        // joinedClubDate should NOT be set here
        // It should be set when admin approves the user

        // Save user first
        User savedUser = userRepository.save(user);

        // Notify admins that a new member requested to join
        notificationService.createForRole(
                "ADMIN",
                "New Member Join Request",
                savedUser.getFullName() + " requested to join the club",
                "MEMBER",
                "AdminApproval",
                null
        );

        // Create matching member profile row
        MemberProfile profile = new MemberProfile();
        profile.setUser(savedUser);

        // Optional profile info
        profile.setNickname(request.getNickname());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());

        // Save member profile
        memberProfileRepository.save(profile);

        return "Registration successful. Waiting for admin approval.";
    }

    /**
     * Login existing user by email + password.
     *
     * Also blocks login if:
     * - account is pending
     * - rejected
     * - inactive
     */
    public LoginResponse login(LoginRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Prevent login if account is still waiting for approval
        if (user.getStatus() == UserStatus.PENDING) {
            throw new RuntimeException("Your account is still pending admin approval");
        }

        // Prevent login if account was rejected
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new RuntimeException("Your account has been rejected. Contact admin");
        }

        // Prevent login if account is inactive
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Your account is inactive. Contact admin");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail());

        // Return login response
        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                token,
                "Login successful"
        );
    }
}