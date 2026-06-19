package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AvailabilityRequest;
import com.gotham.cricket.dto.AvailabilityResponse;
import com.gotham.cricket.dto.AvailabilitySummaryResponse;
import com.gotham.cricket.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Match Availability", description = "Submit and review player availability for matches")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    @Operation(summary = "Mark match availability", description = "Creates or updates the authenticated user's availability for a match.")
    public String markAvailability(Authentication authentication,
                                   @Valid @RequestBody AvailabilityRequest request) {
        String email = authentication.getName();
        return availabilityService.markAvailability(email, request);
    }

    @GetMapping("/match/{matchId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get match availability", description = "Returns player availability records for a match.")
    public List<AvailabilityResponse> getAvailabilityByMatch(@PathVariable Long matchId) {
        return availabilityService.getAvailabilityByMatch(matchId);
    }

    @GetMapping("/match/{matchId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get availability summary", description = "Returns summarized availability counts for a match.")
    public AvailabilitySummaryResponse getAvailabilitySummary(@PathVariable Long matchId) {
        return availabilityService.getAvailabilitySummary(matchId);
    }
}
