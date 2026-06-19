package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AnnouncementRequest;
import com.gotham.cricket.dto.AnnouncementResponse;
import com.gotham.cricket.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Announcements", description = "Create, view, update, pin, and remove club announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create an announcement", description = "Creates an announcement as the authenticated user. Requires ADMIN or CAPTAIN.")
    public String createAnnouncement(Authentication authentication,
                                     @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.createAnnouncement(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Get all announcements", description = "Returns all announcements.")
    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update an announcement", description = "Updates an existing announcement. Requires ADMIN or CAPTAIN.")
    public String updateAnnouncement(@PathVariable Long id,
                                     @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.updateAnnouncement(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete an announcement", description = "Deletes an announcement. Requires ADMIN or CAPTAIN.")
    public String deleteAnnouncement(@PathVariable Long id) {
        return announcementService.deleteAnnouncement(id);
    }
    // Get pinned announcement for home screen
    @GetMapping("/pinned")
    @Operation(summary = "Get pinned announcement", description = "Returns the announcement currently pinned to the home screen.")
    public AnnouncementResponse getPinnedAnnouncement() {
        return announcementService.getPinnedAnnouncement();
    }

    // Pin announcement (admin/captain only)
    @PutMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Pin an announcement", description = "Pins an announcement to the home screen. Requires ADMIN or CAPTAIN.")
    public String pinAnnouncement(@PathVariable Long id) {
        return announcementService.pinAnnouncement(id);
    }

    // Unpin announcement (admin/captain only)
    @PutMapping("/{id}/unpin")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Unpin an announcement", description = "Removes an announcement from the pinned position. Requires ADMIN or CAPTAIN.")
    public String unpinAnnouncement(@PathVariable Long id) {
        return announcementService.unpinAnnouncement(id);
    }
}
