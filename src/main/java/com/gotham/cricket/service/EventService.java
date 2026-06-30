package com.gotham.cricket.service;

import com.gotham.cricket.dto.CreateEventRequest;
import com.gotham.cricket.dto.EventAvailabilityRequest;
import com.gotham.cricket.dto.EventAvailabilityResponse;
import com.gotham.cricket.dto.EventResponse;
import com.gotham.cricket.entity.Event;
import com.gotham.cricket.entity.EventAvailability;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.EventAvailabilityRepository;
import com.gotham.cricket.repository.EventRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventAvailabilityRepository eventAvailabilityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatRoomProvisioningService chatRoomProvisioningService;
    private final S3Service s3Service;

    // Create a new club event
    public String createEvent(String email, CreateEventRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());

        // ✅ Store User object instead of plain email string
        event.setCreatedBy(user);

        eventRepository.save(event);

        // Immediately adds ADMINs + event creator as room admins
        chatRoomProvisioningService.syncEventRoomMembership(event);

        notificationService.createForAllApprovedUsers(
                "New Event Added",
                event.getTitle() + " at " + event.getLocation(),
                "EVENT",
                "Events",
                event.getId()
        );

        return "Event created successfully";
    }

    // Return all events with current user's availability status
    public List<EventResponse> getAllEvents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return eventRepository.findAllByOrderByEventDateAsc()
                .stream()
                .map(event -> {
                    EventAvailability availability = eventAvailabilityRepository
                            .findByEventIdAndUserId(event.getId(), user.getId())
                            .orElse(null);

                    return toResponse(event, availability);
                })
                .toList();
    }

    // Update event details
    public String updateEvent(Long eventId, CreateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());

        eventRepository.save(event);

        notificationService.createForAllApprovedUsers(
                "Event Updated",
                event.getTitle() + " at " + event.getLocation(),
                "EVENT",
                "Events",
                event.getId()
        );

        return "Event updated successfully";
    }

    // Delete event and all its availability records
    @Transactional
    public String deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Delete child rows first to avoid FK constraint violations
        eventAvailabilityRepository.deleteByEventId(eventId);
        eventRepository.delete(event);

        return "Event deleted successfully";
    }

    // Submit or update one user's availability for an event
    @Transactional
    public String submitAvailability(Long eventId, String email, EventAvailabilityRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Reject RSVP updates for events that have already passed.
        if (event.getEventDate() != null && event.getEventDate().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Availability is closed because this event has already passed");
        }

        // Reuse existing record if user already responded, otherwise create new
        EventAvailability availability = eventAvailabilityRepository
                .findByEventIdAndUserId(eventId, user.getId())
                .orElse(new EventAvailability());

        availability.setEvent(event);
        availability.setUser(user);
        availability.setStatus(request.getStatus());
        availability.setMessage(request.getMessage());

        eventAvailabilityRepository.save(availability);

        // Re-sync chat room membership based on updated availability
        chatRoomProvisioningService.syncEventRoomMembership(event);

        return "Event availability submitted successfully";
    }

    // Get all member responses for one event
    public List<EventAvailabilityResponse> getEventAvailability(Long eventId) {
        return eventAvailabilityRepository.findByEventId(eventId)
                .stream()
                .map(a -> new EventAvailabilityResponse(
                        a.getId(),
                        a.getUser().getId(),
                        a.getUser().getFullName(),
                        a.getStatus(),
                        a.getMessage(),
                        s3Service.generateDownloadUrl(a.getUser().getProfileImageKey(), 60)
                ))
                .toList();
    }

    // Get single event with current user's availability status
    public EventResponse getEventById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        EventAvailability availability = eventAvailabilityRepository
                .findByEventIdAndUserId(id, user.getId())
                .orElse(null);

        return toResponse(event, availability);
    }

    // ✅ Helper to build EventResponse from Event + availability
    // Avoids repeating the same mapping logic in every method
    private EventResponse toResponse(Event event, EventAvailability availability) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getCreatedBy().getId(),
                event.getCreatedBy().getFullName(),
                event.getCreatedAt(),
                availability != null ? availability.getStatus() : null
        );
    }
}