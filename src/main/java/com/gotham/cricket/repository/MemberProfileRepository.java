package com.gotham.cricket.repository;

import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
    Optional<MemberProfile> findByUser(User user);
}