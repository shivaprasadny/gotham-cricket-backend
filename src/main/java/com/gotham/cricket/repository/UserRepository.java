package com.gotham.cricket.repository;

import com.gotham.cricket.entity.EmailVerificationToken;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByStatus(UserStatus status);

    List<User> findByStatusIn(List<UserStatus> statuses);

    @Query("SELECT u.email FROM User u WHERE u.id IN :ids")
    List<String> findEmailsByIdIn(@Param("ids") List<Long> ids);
}