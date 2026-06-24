package com.gotham.cricket.service;

import com.gotham.cricket.dto.ForgotPasswordRequest;
import com.gotham.cricket.dto.ForgotPasswordResponse;
import com.gotham.cricket.dto.ResetPasswordRequest;
import com.gotham.cricket.entity.PasswordResetCode;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.PasswordResetCodeRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ Hash the code with SHA-256 before storing
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public ForgotPasswordResponse requestResetCode(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        // Generate plain code — this is what we send to the user
        String plainCode = String.format("%06d", new Random().nextInt(999999));

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(user.getEmail());

        // ✅ Store hashed version — plain code never touches the DB
        resetCode.setCode(hashCode(plainCode));
        resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetCode.setUsed(false);

        passwordResetCodeRepository.save(resetCode);

        // ✅ Return plain code to send to user via email/SMS
        return new ForgotPasswordResponse(
                "Reset code generated successfully",
                plainCode
        );
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        // ✅ Hash the code the user submitted before looking it up in DB
        String hashedCode = hashCode(request.getCode());

        PasswordResetCode resetCode = passwordResetCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByIdDesc(request.getEmail(), hashedCode)
                .orElseThrow(() -> new RuntimeException("Invalid reset code"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset code has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetCode.setUsed(true);
        passwordResetCodeRepository.save(resetCode);

        return "Password reset successful";
    }
}