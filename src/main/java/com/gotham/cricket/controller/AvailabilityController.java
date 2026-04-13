package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AvailabilityRequest;
import com.gotham.cricket.dto.AvailabilityResponse;
import com.gotham.cricket.dto.AvailabilitySummaryResponse;
import com.gotham.cricket.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public String markAvailability(Authentication authentication,
                                   @Valid @RequestBody AvailabilityRequest request) {
        String email = authentication.getName();
        return availabilityService.markAvailability(email, request);
    }

    @GetMapping("/match/{matchId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<AvailabilityResponse> getAvailabilityByMatch(@PathVariable Long matchId) {
        return availabilityService.getAvailabilityByMatch(matchId);
    }

    @GetMapping("/match/{matchId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public AvailabilitySummaryResponse getAvailabilitySummary(@PathVariable Long matchId) {
        return availabilityService.getAvailabilitySummary(matchId);
    }
}