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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createTeam(@Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public TeamResponse getTeamById(@PathVariable Long teamId) {
        return teamService.getTeamById(teamId);
    }

    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String addMemberToTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        return teamService.addMemberToTeam(teamId, userId);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String removeMemberFromTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        return teamService.removeMemberFromTeam(teamId, userId);
    }

    @GetMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<TeamMemberResponse> getTeamMembers(@PathVariable Long teamId) {
        return teamService.getTeamMembers(teamId);
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateTeam(@PathVariable Long teamId,
                             @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(teamId, request);
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteTeam(@PathVariable Long teamId) {
        return teamService.deleteTeam(teamId);
    }

    @GetMapping("/{teamId}/available-members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<AvailableTeamMemberResponse> getAvailableMembers(@PathVariable Long teamId) {
        return teamService.getAvailableMembers(teamId);
    }

}