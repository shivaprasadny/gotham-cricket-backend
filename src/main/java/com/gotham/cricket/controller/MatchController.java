package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchRepository matchRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createMatch(Authentication authentication, @RequestBody MatchRequest request) {
        return matchService.createMatch(authentication.getName(), request);
    }

    @GetMapping
    public List<MatchResponse> getAllMatches(Authentication authentication) {
        return matchService.getAllMatches(authentication.getName());
    }

    @GetMapping("/{id}")
    public MatchResponse getMatchById(@PathVariable Long id, Authentication authentication) {
        return matchService.getMatchById(id, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateMatch(@PathVariable Long id, @RequestBody MatchRequest request) {
        return matchService.updateMatch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteMatch(@PathVariable Long id) {
        return matchService.deleteMatch(id);
    }






    @GetMapping("/upcoming")
    public List<Match> getUpcomingMatches() {
        return matchService.getUpcomingMatches();
    }
}