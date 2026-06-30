package com.gotham.cricket.service;

import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    // User repository for member operations
    private final UserRepository userRepository;

    // Notification service for backend notifications
    private final NotificationService notificationService;
    private final ChatRoomProvisioningService chatRoomProvisioningService;
    private final S3Service s3Service;

    // Get all pending members
    public List<UserApprovalResponse> getPendingMembers() {
        return userRepository.findByStatus(UserStatus.PENDING)
                .stream()
                .map(user -> new UserApprovalResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getStatus(),
                        s3Service.generateDownloadUrl(user.getProfileImageKey(), 60)
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
        chatRoomProvisioningService.addApprovedMemberToSharedRooms(user);

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
                        user.getStatus(),
                        s3Service.generateDownloadUrl(user.getProfileImageKey(), 60)
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
        chatRoomProvisioningService.addApprovedMemberToSharedRooms(user);

        return "User activated successfully";
    }
}
