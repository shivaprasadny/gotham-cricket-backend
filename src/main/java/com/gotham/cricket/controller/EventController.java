package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateEventRequest;
import com.gotham.cricket.dto.EventAvailabilityRequest;
import com.gotham.cricket.dto.EventAvailabilityResponse;
import com.gotham.cricket.dto.EventResponse;
import com.gotham.cricket.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createEvent(
            Authentication authentication,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return eventService.createEvent(authentication.getName(), request);
    }

    @GetMapping
    public List<EventResponse> getAllEvents(Authentication authentication) {
        return eventService.getAllEvents(authentication.getName());
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteEvent(@PathVariable Long eventId) {
        return eventService.deleteEvent(eventId);
    }

    @PostMapping("/{eventId}/availability")
    public String submitAvailability(
            @PathVariable Long eventId,
            Authentication authentication,
            @Valid @RequestBody EventAvailabilityRequest request
    ) {
        return eventService.submitAvailability(eventId, authentication.getName(), request);
    }

    @GetMapping("/{eventId}/availability")

    public List<EventAvailabilityResponse> getEventAvailability(@PathVariable Long eventId) {
        return eventService.getEventAvailability(eventId);
    }
    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable Long id, Principal principal) {
        return eventService.getEventById(id, principal.getName());
    }

}