package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AvailabilityRequest;
import com.gotham.cricket.dto.AvailabilityResponse;
import com.gotham.cricket.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public String markAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return availabilityService.markAvailability(request);
    }

    @GetMapping("/match/{matchId}")
    public List<AvailabilityResponse> getAvailabilityByMatch(@PathVariable Long matchId) {
        return availabilityService.getAvailabilityByMatch(matchId);
    }
}