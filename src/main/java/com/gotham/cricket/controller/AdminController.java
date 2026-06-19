package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ApproveUserRequest;
import com.gotham.cricket.dto.UpdateUserRoleRequest;
import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Administration", description = "Approve members and manage user roles and account status")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/pending-members")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending members", description = "Returns users awaiting approval. Requires ADMIN.")
    public List<UserApprovalResponse> getPendingMembers() {
        return adminService.getPendingMembers();
    }

    @PutMapping("/members/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a member", description = "Approves a pending user with the requested role, defaulting to PLAYER. Requires ADMIN.")
    public String approveMember(@PathVariable Long id,
                                @RequestBody(required = false) ApproveUserRequest request) {
        Role role = Role.PLAYER;

        if (request != null && request.getRole() != null) {
            role = request.getRole();
        }

        return adminService.approveMember(id, role);
    }

    @PutMapping("/members/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a member", description = "Rejects a pending user registration. Requires ADMIN.")
    public String rejectMember(@PathVariable Long id) {
        return adminService.rejectMember(id);
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Get approved members", description = "Returns all approved members. Requires ADMIN or CAPTAIN.")
    public List<UserApprovalResponse> getAllMembers() {
        return adminService.getAllApprovedMembers();
    }

    @PutMapping("/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update member role", description = "Changes an approved member's role. Requires ADMIN.")
    public String updateMemberRole(@PathVariable Long id,
                                   @RequestBody UpdateUserRoleRequest request) {
        return adminService.updateMemberRole(id, request.getRole());
    }

    @PutMapping("/members/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a member", description = "Deactivates a user account. Requires ADMIN.")
    public String deactivateUser(@PathVariable Long userId) {
        return adminService.deactivateUser(userId);
    }

    @PutMapping("/members/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a member", description = "Reactivates a user account. Requires ADMIN.")
    public String activateUser(@PathVariable Long userId) {
        return adminService.activateUser(userId);
    }
}
