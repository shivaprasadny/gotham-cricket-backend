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

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordResponse requestResetCode(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String code = String.format("%06d", new Random().nextInt(999999));

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(user.getEmail());
        resetCode.setCode(code);
        resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetCode.setUsed(false);

        passwordResetCodeRepository.save(resetCode);

        return new ForgotPasswordResponse(
                "Reset code generated successfully",
                code
        );
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        PasswordResetCode resetCode = passwordResetCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByIdDesc(request.getEmail(), request.getCode())
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