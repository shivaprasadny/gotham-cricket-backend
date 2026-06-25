package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateEventRequest;
import com.gotham.cricket.dto.EventAvailabilityRequest;
import com.gotham.cricket.dto.EventAvailabilityResponse;
import com.gotham.cricket.dto.EventResponse;
import com.gotham.cricket.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping({"/api/events", "/api/v1/events"})
@RequiredArgsConstructor
@Tag(name = "Events", description = "Manage club events and event attendance availability")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create an event", description = "Creates a club event. Requires ADMIN or CAPTAIN.")
    public String createEvent(
            Authentication authentication,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return eventService.createEvent(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Returns events with availability information for the authenticated user.")
    public List<EventResponse> getAllEvents(Authentication authentication) {
        return eventService.getAllEvents(authentication.getName());
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update an event", description = "Updates an existing event. Requires ADMIN or CAPTAIN.")
    public String updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete an event", description = "Deletes an event. Requires ADMIN or CAPTAIN.")
    public String deleteEvent(@PathVariable Long eventId) {
        return eventService.deleteEvent(eventId);
    }

    @PostMapping("/{eventId}/availability")
    @Operation(summary = "Submit event availability", description = "Creates or updates the authenticated user's availability for an event.")
    public String submitAvailability(
            @PathVariable Long eventId,
            Authentication authentication,
            @Valid @RequestBody EventAvailabilityRequest request
    ) {
        return eventService.submitAvailability(eventId, authentication.getName(), request);
    }

    @GetMapping("/{eventId}/availability")
    @Operation(summary = "Get event availability", description = "Returns member availability records for an event.")
    public List<EventAvailabilityResponse> getEventAvailability(@PathVariable Long eventId) {
        return eventService.getEventAvailability(eventId);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get an event by ID", description = "Returns one event with availability information for the authenticated user.")
    public EventResponse getEventById(@PathVariable Long id, Principal principal) {
        return eventService.getEventById(id, principal.getName());
    }

}
