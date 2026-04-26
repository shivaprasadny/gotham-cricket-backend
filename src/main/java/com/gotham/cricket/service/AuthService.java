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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    /**
     * Register new user.
     * New users are saved as PLAYER + PENDING until admin approves them.
     */
    public String register(RegisterRequest request) {

        // Clean email before saving/checking
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (request.getPassword() == null || request.getPassword().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        // Prevent duplicate email, ignoring uppercase/lowercase
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        String firstName = request.getFirstName() == null
                ? ""
                : request.getFirstName().trim();

        String lastName = request.getLastName() == null
                ? ""
                : request.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        User user = new User();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setFullName(fullName);

        // Save normalized email
        user.setEmail(email);

        // Always save encoded password
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));

        user.setRole(Role.PLAYER);
        user.setStatus(UserStatus.PENDING);

        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        User savedUser = userRepository.save(user);

        // Notify admins about new registration
        notificationService.createForRole(
                "ADMIN",
                "New Member Join Request",
                savedUser.getFullName() + " requested to join the club",
                "MEMBER",
                "AdminApproval",
                null
        );

        MemberProfile profile = new MemberProfile();
        profile.setUser(savedUser);
        profile.setNickname(request.getNickname());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());

        memberProfileRepository.save(profile);

        return "Registration successful. Waiting for admin approval.";
    }

    /**
     * Login user using email + password.
     * Blocks users who are not approved yet.
     */
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        String rawPassword = request.getPassword() == null
                ? ""
                : request.getPassword().trim();

        if (email.isBlank() || rawPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password are required"
            );
        }

        // Find user ignoring uppercase/lowercase
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        // Check encoded password
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // User exists and password is correct, now check approval status
        if (user.getStatus() == UserStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is still pending admin approval"
            );
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account has been rejected. Contact admin"
            );
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is inactive. Contact admin"
            );
        }

        String token = jwtService.generateToken(user.getEmail());

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