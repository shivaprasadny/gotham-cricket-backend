package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AnnouncementRequest;
import com.gotham.cricket.dto.AnnouncementResponse;
import com.gotham.cricket.service.AnnouncementService;
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
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createAnnouncement(Authentication authentication,
                                     @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.createAnnouncement(authentication.getName(), request);
    }

    @GetMapping
    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateAnnouncement(@PathVariable Long id,
                                     @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.updateAnnouncement(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteAnnouncement(@PathVariable Long id) {
        return announcementService.deleteAnnouncement(id);
    }
}