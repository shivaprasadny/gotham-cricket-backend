package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateLeagueRequest;
import com.gotham.cricket.dto.LeagueResponse;
import com.gotham.cricket.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeagueController {

    private final LeagueService leagueService;

    // Admin / Captain can create league
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createLeague(@RequestBody CreateLeagueRequest request) {
        return leagueService.createLeague(request);
    }

    // All logged in users can view leagues
    @GetMapping
    public List<LeagueResponse> getAllLeagues() {
        return leagueService.getAllLeagues();
    }

    // Get one league details
    @GetMapping("/{id}")
    public LeagueResponse getLeagueById(@PathVariable Long id) {
        return leagueService.getLeagueById(id);
    }

    // Admin / Captain can update league
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateLeague(
            @PathVariable Long id,
            @RequestBody CreateLeagueRequest request
    ) {
        return leagueService.updateLeague(id, request);
    }

    // Admin / Captain can delete league
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteLeague(@PathVariable Long id) {
        return leagueService.deleteLeague(id);
    }
}