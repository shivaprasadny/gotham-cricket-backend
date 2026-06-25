package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MemberResponse;
import com.gotham.cricket.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/members", "/api/v1/members"})
@RequiredArgsConstructor
@Tag(name = "Members", description = "View approved club members and member profiles")
public class MemberController {

    private final MemberService memberService;

    // Everyone can view approved members list
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get approved members", description = "Returns all approved club members.")
    public List<MemberResponse> getAllApprovedMembers() {
        return memberService.getAllApprovedMembers();
    }

    // Admin/Captain only can open one member detail
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get a member by ID", description = "Returns details for one approved member.")
    public MemberResponse getMemberById(@PathVariable Long userId) {
        return memberService.getMemberById(userId);
    }
}
