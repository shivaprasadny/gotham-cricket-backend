package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateEventRequest;
import com.gotham.cricket.dto.EventAvailabilityRequest;
import com.gotham.cricket.dto.EventAvailabilityResponse;
import com.gotham.cricket.dto.EventResponse;
import com.gotham.cricket.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    // Admin / Captain can create event
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createEvent(Authentication authentication,
                              @RequestBody CreateEventRequest request) {
        return eventService.createEvent(authentication.getName(), request);
    }

    // All logged-in users can see events
    @GetMapping
    public List<EventResponse> getAllEvents(Authentication authentication) {
        return eventService.getAllEvents(authentication.getName());
    }

    // All users can submit their response
    @PutMapping("/{eventId}/availability")
    public String submitAvailability(@PathVariable Long eventId,
                                     Authentication authentication,
                                     @RequestBody EventAvailabilityRequest request) {
        return eventService.submitAvailability(eventId, authentication.getName(), request);
    }

    // Admin / Captain can view all responses
    @GetMapping("/{eventId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<EventAvailabilityResponse> getEventAvailability(@PathVariable Long eventId) {
        return eventService.getEventAvailability(eventId);
    }
}