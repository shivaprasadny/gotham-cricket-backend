package com.gotham.cricket.service;

import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
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

    public String approveMember(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.APPROVED);
        user.setRole(role != null ? role : Role.PLAYER);

        userRepository.save(user);

        return "User approved successfully as " + user.getRole().name();
    }

    public String rejectMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        return "User rejected successfully";
    }

    public List<UserApprovalResponse> getAllApprovedMembers() {
        return userRepository.findByStatus(UserStatus.APPROVED)
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

    public String updateMemberRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.APPROVED) {
            throw new RuntimeException("Only approved users can have role changed");
        }

        user.setRole(role);
        userRepository.save(user);

        return "User role updated to " + role.name();
    }
}