package com.gotham.cricket.service;

import com.gotham.cricket.dto.ForgotPasswordRequest;
import com.gotham.cricket.dto.LoginRequest;
import com.gotham.cricket.dto.LoginResponse;
import com.gotham.cricket.dto.RegisterRequest;
import com.gotham.cricket.dto.ResetPasswordRequest;
import com.gotham.cricket.dto.VerifyEmailCodeRequest;
import com.gotham.cricket.entity.EmailVerificationToken;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.PasswordResetCode;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.EmailVerificationTokenRepository;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.PasswordResetCodeRepository;
import com.gotham.cricket.repository.UserRepository;
import com.gotham.cricket.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final S3Service s3Service;

    // =========================
    // REGISTER USER
    // =========================
    @Transactional
    public String register(RegisterRequest request) {

        // Normalize email to lowercase and trim whitespace
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

        // ✅ Rate limit registration — max 3 attempts per 60 minutes per email
        // Prevents spam account creation from the same email
        String registerKey = "REGISTER:" + email;
        if (!rateLimitService.isAllowed(registerKey, 3, 60)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many registration attempts. Please try again later."
            );
        }

        // Prevent duplicate accounts with same email
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();

        // Create new user — starts with EMAIL_PENDING until they verify email
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

        // Create linked member profile with cricket-specific details
        MemberProfile profile = new MemberProfile();
        profile.setUser(savedUser);
        profile.setNickname(request.getNickname());
        profile.setCountryCode(request.getCountryCode());
        profile.setPhone(request.getPhone());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setPlayerType(request.getPlayerType());
        profile.setJerseyNumber(request.getJerseyNumber());
        // Privacy defaults are true — set explicitly for clarity
        profile.setShowEmail(true);
        profile.setShowPhone(true);
        profile.setShowWhatsApp(true);

        memberProfileRepository.save(profile);

        // Generate 6-digit email verification code
        String code = generateSixDigitCode();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(savedUser);
        verificationToken.setToken(code);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationToken.setUsed(false);

        emailVerificationTokenRepository.save(verificationToken);

        // Send verification email — if this fails, @Transactional rolls back
        // the user, profile, and token so DB stays clean
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

        // Validate input before hitting the database
        if (email.isBlank() || rawPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password required"
            );
        }

        // ✅ Rate limit login — max 5 attempts per 15 minutes per email
        // Prevents brute force password attacks
        String loginKey = "LOGIN:" + email;
        if (!rateLimitService.isAllowed(loginKey, 5, 15)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again in 15 minutes."
            );
        }

        // Find user by email — return generic error to avoid user enumeration
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                ));

        // Verify password matches stored hash
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        // Block users who have not verified their email yet
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

        // ✅ Clear login rate limit on successful login
        // so legitimate users are not locked out after password change etc.
        rateLimitService.clear(loginKey);

        // Generate JWT token for authenticated session
        String token = jwtService.generateToken(user.getEmail());

        String profileImageUrl = null;
        if (user.getProfileImageKey() != null) {
            try { profileImageUrl = s3Service.generateDownloadUrl(user.getProfileImageKey(), 60); }
            catch (Exception ignored) {}
        }

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                token,
                "Login successful",
                profileImageUrl
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

        // Prevent reuse of already verified codes
        if (verificationToken.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code already used"
            );
        }

        // Check code has not expired (10 minute window)
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code expired"
            );
        }

        // Check code matches
        if (!verificationToken.getToken().equals(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid verification code"
            );
        }

        // Email verified — move to PENDING status, waiting for admin approval
        user.setStatus(UserStatus.PENDING);
        verificationToken.setUsed(true);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);

        // Notify all admins that a new member is waiting for approval
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

        // Only resend if user is still in EMAIL_PENDING status
        if (user.getStatus() != UserStatus.EMAIL_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email verification is not required for this account"
            );
        }

        // Generate fresh 6-digit code
        String code = generateSixDigitCode();

        // Reuse existing token record if one exists, otherwise create new
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

        // ✅ Rate limit forgot password — max 3 requests per 15 minutes per email
        // Prevents email spam attacks
        String key = "FORGOT_PASSWORD:" + email;
        if (!rateLimitService.isAllowed(key, 3, 15)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many password reset requests. Please try again later."
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Generate plain code — this is what we send to the user by email
        String plainCode = generateSixDigitCode();

        // ✅ Hash before saving — plain code never touches the database
        // If DB is compromised, attacker cannot use the stored hash to reset passwords
        String hashedCode = hashCode(plainCode);

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(user.getEmail());
        resetCode.setCode(hashedCode);
        resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetCode.setUsed(false);

        passwordResetCodeRepository.save(resetCode);

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
                    """.formatted(plainCode)
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

        // ✅ Rate limit reset attempts — max 5 attempts per 15 minutes per email
        // Prevents brute force guessing of reset codes
        String key = "RESET_CODE_VERIFY:" + email;
        if (!rateLimitService.isAllowed(key, 5, 15)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many reset code attempts. Please try again later."
            );
        }

        // Verify user exists before doing anything else
        userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // ✅ Hash the submitted code before looking up in DB
        // Must match the hashed version stored during forgotPassword
        String hashedCode = hashCode(code);

        PasswordResetCode resetCode = passwordResetCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByIdDesc(email, hashedCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid reset code"
                ));

        // Check code has not expired (10 minute window)
        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset code expired"
            );
        }

        // Save new encoded password
        User user = userRepository.findByEmailIgnoreCase(email).get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark reset code as used so it cannot be reused
        resetCode.setUsed(true);
        passwordResetCodeRepository.save(resetCode);

        // ✅ Clear rate limits after successful reset
        rateLimitService.clear("FORGOT_PASSWORD:" + email);
        rateLimitService.clear("RESET_CODE_VERIFY:" + email);

        return "Password reset successful.";
    }

    // =========================
    // VERIFY EMAIL VIA LINK (legacy)
    // Keep for backward compatibility with old email link flow.
    // Can be removed once all users are on 6-digit OTP flow.
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
    // HELPERS
    // =========================

    // Generate a cryptographically secure 6-digit code
    // Uses SecureRandom instead of Random for security
    private String generateSixDigitCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    // SHA-256 hash utility for reset codes
    // One-way hash — cannot be reversed to get original code
    private String hashCode(String code) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}