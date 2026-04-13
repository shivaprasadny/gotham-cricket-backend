package com.gotham.cricket.service;

import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public List<UserApprovalResponse> getPendingMembers() {
        return userRepository.findByStatus(UserStatus.PENDING)
                .stream()
                .map(user -> new UserApprovalResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getStatus()
                ))
                .toList();
    }

    public String approveMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);

        return "User approved successfully";
    }

    public String rejectMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        return "User rejected successfully";
    }
}