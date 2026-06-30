package com.gotham.cricket.service;

import com.gotham.cricket.dto.AnnouncementRequest;
import com.gotham.cricket.dto.AnnouncementResponse;
import com.gotham.cricket.entity.Announcement;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.AnnouncementRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public String createAnnouncement(String email, AnnouncementRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setCreatedBy(user.getFullName());

        announcementRepository.save(announcement);

        // Send in-app notification + mobile push notification
        notificationService.createForAllUsers(
                "Announcement Posted",
                request.getTitle(),
                "ANNOUNCEMENT",
                "AnnouncementDetails",
                announcement.getId()
        );

        return "Announcement created successfully";
    }

    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(a -> new AnnouncementResponse(
                        a.getId(),
                        a.getTitle(),
                        a.getMessage(),
                        a.getCreatedBy(),
                        a.getCreatedAt(),
                        a.isPinned()
                ))
                .toList();
    }

    public String updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found with id: " + id));

        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());

        announcementRepository.save(announcement);

        return "Announcement updated successfully";
    }

    public String deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found with id: " + id));

        announcementRepository.delete(announcement);

        return "Announcement deleted successfully";
    }


    // Returns the currently pinned announcement, if one exists
    public AnnouncementResponse getPinnedAnnouncement() {
        Announcement announcement = announcementRepository.findByPinnedTrue().orElse(null);

        if (announcement == null) {
            return null;
        }

        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getMessage(),
                announcement.getCreatedBy(),
                announcement.getCreatedAt(),
                announcement.isPinned()
        );
    }

    // Pins one announcement and unpins all others
    public String pinAnnouncement(Long announcementId) {

        // First, unpin any currently pinned announcement
        announcementRepository.findByPinnedTrue().ifPresent(existingPinned -> {
            existingPinned.setPinned(false);
            announcementRepository.save(existingPinned);
        });

        // Then pin the selected announcement
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.setPinned(true);
        announcementRepository.save(announcement);

        return "Announcement pinned successfully";
    }

    // Removes pin from one announcement
    public String unpinAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.setPinned(false);
        announcementRepository.save(announcement);

        return "Announcement unpinned successfully";
    }


}