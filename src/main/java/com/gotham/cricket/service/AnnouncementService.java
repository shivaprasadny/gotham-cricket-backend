package com.gotham.cricket.service;

import com.gotham.cricket.dto.AnnouncementRequest;
import com.gotham.cricket.dto.AnnouncementResponse;
import com.gotham.cricket.entity.Announcement;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.AnnouncementRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public String createAnnouncement(String email, AnnouncementRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setCreatedBy(user.getFullName());

        announcementRepository.save(announcement);

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
                        a.getCreatedAt()
                ))
                .toList();
    }
}