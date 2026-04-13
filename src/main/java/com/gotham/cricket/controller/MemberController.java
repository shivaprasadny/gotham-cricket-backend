package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MemberResponse;
import com.gotham.cricket.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<MemberResponse> getAllApprovedMembers() {
        return memberService.getAllApprovedMembers();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public MemberResponse getMemberById(@PathVariable Long userId) {
        return memberService.getMemberById(userId);
    }
}