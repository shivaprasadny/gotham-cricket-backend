package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findTopByEmailAndCodeAndUsedFalseOrderByIdDesc(String email, String code);
}