package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ApproveUserRequest;
import com.gotham.cricket.dto.UpdateUserRoleRequest;
import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/pending-members")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserApprovalResponse> getPendingMembers() {
        return adminService.getPendingMembers();
    }

    @PutMapping("/members/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
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
    public String rejectMember(@PathVariable Long id) {
        return adminService.rejectMember(id);
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<UserApprovalResponse> getAllMembers() {
        return adminService.getAllApprovedMembers();
    }

    @PutMapping("/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateMemberRole(@PathVariable Long id,
                                   @RequestBody UpdateUserRoleRequest request) {
        return adminService.updateMemberRole(id, request.getRole());
    }
}