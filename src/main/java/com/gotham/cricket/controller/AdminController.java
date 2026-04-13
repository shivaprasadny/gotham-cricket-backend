package com.gotham.cricket.controller;

import com.gotham.cricket.dto.UserApprovalResponse;
import com.gotham.cricket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/pending-members")
    public List<UserApprovalResponse> getPendingMembers() {
        return adminService.getPendingMembers();
    }

    @PutMapping("/members/{id}/approve")
    public String approveMember(@PathVariable Long id) {
        return adminService.approveMember(id);
    }

    @PutMapping("/members/{id}/reject")
    public String rejectMember(@PathVariable Long id) {
        return adminService.rejectMember(id);
    }
}