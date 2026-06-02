package com.gotham.cricket.service;

import com.gotham.cricket.dto.ForgotPasswordRequest;
import com.gotham.cricket.dto.LoginRequest;
import com.gotham.cricket.dto.LoginResponse;
import com.gotham.cricket.dto.RegisterRequest;
import com.gotham.cricket.dto.ResetPasswordRequest;
import com.gotham.cricket.dto.VerifyEmailCodeRequest;
import com.gotham.cricket.entity.EmailVerificationToken;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.EmailVerificationTokenRepository;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import com.gotham.cricket.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;

    // =========================
    // REGISTER USER
    // =========================
    @Transactional
    public String register(RegisterRequest request) {

        // Normalize email
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        // Validate required fields
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (request.getPassword() == null || request.getPassword().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        // Prevent duplicate registration
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();

        // Create user with EMAIL_PENDING status
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setFullName((firstName + " " + lastName).trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setRole(Role.PLAYER);
        user.setStatus(UserStatus.EMAIL_PENDING);
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        User savedUser = userRepository.save(user);

        // Create member profile
        MemberProfile profile = new MemberProfile();
        profile.setUser(savedUser);
        profile.setNickname(request.getNickname());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());

        memberProfileRepository.save(profile);

        // Create 6-digit verification code
        String code = generateSixDigitCode();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(savedUser);
        verificationToken.setToken(code);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationToken.setUsed(false);

        emailVerificationTokenRepository.save(verificationToken);

        // Send verification email
        // If email fails, @Transactional rolls back user/profile/token
        try {
            emailService.sendEmail(
                    savedUser.getEmail(),
                    "Gotham Cricket Email Verification",
                    """
                    Hello,

                    Your Gotham Cricket verification code is: %s

                    This code expires in 10 minutes.

                    If you did not request this, please ignore this email.

                    Thanks,
                    Gotham Cricket Club
                    """.formatted(code)
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send verification email. Please try again later."
            );
        }

        return "Registration successful. Please verify your email.";
    }

    // =========================
    // LOGIN
    // =========================
    public LoginResponse login(LoginRequest request) {

        // Normalize input
        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        String rawPassword = request.getPassword() == null
                ? ""
                : request.getPassword().trim();

        // Validate input
        if (email.isBlank() || rawPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password required"
            );
        }

        // Find user
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                ));

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        // Block users who have not verified email
        if (user.getStatus() == UserStatus.EMAIL_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Please verify your email first"
            );
        }

        // Block users waiting for admin approval
        if (user.getStatus() == UserStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Waiting for admin approval"
            );
        }

        // Block rejected users
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account rejected"
            );
        }

        // Block inactive users
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account inactive"
            );
        }

        // Generate JWT token
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

    // =========================
    // VERIFY EMAIL CODE
    // =========================
    public String verifyEmailCode(VerifyEmailCodeRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        String code = request.getCode() == null
                ? ""
                : request.getCode().trim();

        if (email.isBlank() || code.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and verification code are required"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByUser(user)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Verification code not found"
                        ));

        if (verificationToken.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code already used"
            );
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code expired"
            );
        }

        if (!verificationToken.getToken().equals(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid verification code"
            );
        }

        // Email verified, now wait for admin approval
        user.setStatus(UserStatus.PENDING);
        verificationToken.setUsed(true);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);

        // Notify admins only after email is verified
        notificationService.createForRole(
                "ADMIN",
                "New Member Join Request",
                user.getFullName() + " verified email and requested to join the club",
                "MEMBER",
                "AdminApproval",
                null
        );

        return "Email verified successfully. Please wait for admin approval.";
    }

    // =========================
    // RESEND VERIFICATION CODE
    // =========================
    public String resendVerificationCode(String emailInput) {

        String email = emailInput == null
                ? ""
                : emailInput.trim().toLowerCase();

        if (email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.EMAIL_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email verification is not required for this account"
            );
        }

        String code = generateSixDigitCode();

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByUser(user)
                        .orElse(new EmailVerificationToken());

        verificationToken.setUser(user);
        verificationToken.setToken(code);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationToken.setUsed(false);

        emailVerificationTokenRepository.save(verificationToken);

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Gotham Cricket Verification Code",
                    """
                    Hello,

                    Your new Gotham Cricket verification code is: %s

                    This code expires in 10 minutes.

                    If you did not request this, please ignore this email.

                    Thanks,
                    Gotham Cricket Club
                    """.formatted(code)
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to resend verification code. Please try again later."
            );
        }

        return "Verification code resent successfully.";
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    public String forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        String code = generateSixDigitCode();

        user.setPasswordResetCode(code);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Gotham Cricket Password Reset Code",
                    """
                    Hello,

                    Your Gotham Cricket password reset code is: %s

                    This code expires in 10 minutes.

                    If you did not request this, please ignore this email.

                    Thanks,
                    Gotham Cricket Club
                    """.formatted(code)
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send password reset email. Please try again later."
            );
        }

        return "Password reset code sent to your email.";
    }

    // =========================
    // RESET PASSWORD
    // =========================
    public String resetPassword(ResetPasswordRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        String code = request.getCode() == null
                ? ""
                : request.getCode().trim();

        String newPassword = request.getNewPassword() == null
                ? ""
                : request.getNewPassword().trim();

        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email, code, and new password are required"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getPasswordResetCode() == null ||
                !user.getPasswordResetCode().equals(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reset code"
            );
        }

        if (user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset code expired"
            );
        }

        // Save new encoded password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Clear reset fields after successful reset
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);

        return "Password reset successful.";
    }

    // =========================
    // OLD LINK VERIFY METHOD
    // Keep only if your old link endpoint still exists.
    // You can remove this later if you fully use 6-digit OTP.
    // =========================
    public String verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid token"
                        ));

        if (verificationToken.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token already used"
            );
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token expired"
            );
        }

        User user = verificationToken.getUser();

        user.setStatus(UserStatus.PENDING);
        verificationToken.setUsed(true);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);

        return "Email verified. Waiting for admin approval.";
    }

    // =========================
    // GENERATE 6-DIGIT CODE
    // =========================
    private String generateSixDigitCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}