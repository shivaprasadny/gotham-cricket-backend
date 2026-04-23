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

    // User repository for member operations
    private final UserRepository userRepository;

    // Notification service for backend notifications
    private final NotificationService notificationService;

    // Get all pending members
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

    // Approve a pending member and send admin-only notification
    public String approveMember(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.APPROVED);
        user.setRole(role != null ? role : Role.PLAYER);

        if (user.getJoinedClubDate() == null) {
            user.setJoinedClubDate(java.time.LocalDate.now());
        }

        userRepository.save(user);

        // Notify admins only that a member was approved
        notificationService.createForRole(
                "ADMIN",
                "Member Approved",
                user.getFullName() + " was approved as " + user.getRole().name(),
                "MEMBER",
                "AdminApproval",
                null
        );

        return "User approved successfully as " + user.getRole().name();
    }

    // Reject a pending member
    public String rejectMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        return "User rejected successfully";
    }

    // Get all approved/inactive members
    public List<UserApprovalResponse> getAllApprovedMembers() {
        return userRepository.findByStatusIn(List.of(UserStatus.APPROVED, UserStatus.INACTIVE))
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

    // Update role for approved member
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

    // Deactivate an approved user
    public String deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        return "User deactivated successfully";
    }

    // Activate an inactive user
    public String activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);

        return "User activated successfully";
    }
}