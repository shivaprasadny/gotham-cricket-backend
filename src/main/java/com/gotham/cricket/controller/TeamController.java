package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AvailableTeamMemberResponse;
import com.gotham.cricket.dto.TeamMemberResponse;
import com.gotham.cricket.dto.TeamRequest;
import com.gotham.cricket.dto.TeamResponse;
import com.gotham.cricket.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeamController {

    private final TeamService teamService;

    // Admin only - create new team
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createTeam(@Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(request);
    }

    // Everyone can view teams
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    // Everyone can view single team details
    @GetMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public TeamResponse getTeamById(@PathVariable Long teamId) {
        return teamService.getTeamById(teamId);
    }

    // Captain/Admin can add members to team
    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String addMemberToTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        return teamService.addMemberToTeam(teamId, userId);
    }

    // Captain/Admin can remove members from team
    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String removeMemberFromTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        return teamService.removeMemberFromTeam(teamId, userId);
    }

    // Everyone can view team members
    @GetMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public List<TeamMemberResponse> getTeamMembers(@PathVariable Long teamId) {
        return teamService.getTeamMembers(teamId);
    }

    // Admin only - edit team info like name/description
    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateTeam(@PathVariable Long teamId,
                             @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(teamId, request);
    }

    // Admin only - delete team
    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTeam(@PathVariable Long teamId) {
        return teamService.deleteTeam(teamId);
    }

    // Captain/Admin can see available members to add into team
    @GetMapping("/{teamId}/available-members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<AvailableTeamMemberResponse> getAvailableMembers(@PathVariable Long teamId) {
        return teamService.getAvailableMembers(teamId);
    }
}