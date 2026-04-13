package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MatchSquadRequest;
import com.gotham.cricket.dto.MatchSquadResponse;
import com.gotham.cricket.service.MatchSquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches/{matchId}/squad")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MatchSquadController {

    private final MatchSquadService matchSquadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String addOrUpdateSquadMember(@PathVariable Long matchId,
                                         @Valid @RequestBody MatchSquadRequest request) {
        return matchSquadService.addOrUpdateSquadMember(matchId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<MatchSquadResponse> getSquadByMatch(@PathVariable Long matchId) {
        return matchSquadService.getSquadByMatch(matchId);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String removeSquadMember(@PathVariable Long matchId,
                                    @PathVariable Long userId) {
        return matchSquadService.removeSquadMember(matchId, userId);
    }
}