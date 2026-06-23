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
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventAvailabilityRepository eventAvailabilityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatRoomProvisioningService chatRoomProvisioningService;

    // Create a new club event
    public String createEvent(String email, CreateEventRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setCreatedBy(user.getFullName());

        eventRepository.save(event);
        chatRoomProvisioningService.ensureEventRoom(event);
        notificationService.createForAllApprovedUsers(
                "New Event Added",
                event.getTitle() + " at " + event.getLocation(),
                "EVENT",
                "Events",
                event.getId()
        );

        return "Event created successfully";
    }

    // Return all events with current user's response
    public List<EventResponse> getAllEvents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return eventRepository.findAllByOrderByEventDateAsc()
                .stream()
                .map(event -> {
                    EventAvailability availability = eventAvailabilityRepository
                            .findByEventIdAndUserId(event.getId(), user.getId())
                            .orElse(null);

                    return new EventResponse(
                            event.getId(),
                            event.getTitle(),
                            event.getDescription(),
                            event.getEventDate(),
                            event.getLocation(),
                            event.getCreatedBy(),
                            event.getCreatedAt(),
                            availability != null ? availability.getStatus() : null
                    );
                })
                .toList();
    }

    // Update event
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

    // Delete event
    @Transactional
    public String deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // delete child rows first
        eventAvailabilityRepository.deleteByEventId(eventId);

        // then delete event
        eventRepository.delete(event);

        return "Event deleted successfully";
    }

    // Submit or update one user's event response
    @Transactional
    public String submitAvailability(Long eventId, String email, EventAvailabilityRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        EventAvailability availability = eventAvailabilityRepository
                .findByEventIdAndUserId(eventId, user.getId())
                .orElse(new EventAvailability());

        availability.setEvent(event);
        availability.setUser(user);
        availability.setStatus(request.getStatus());
        availability.setMessage(request.getMessage());

        eventAvailabilityRepository.save(availability);
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
                        a.getMessage()
                ))
                .toList();
    }
    public EventResponse getEventById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        EventAvailability availability = eventAvailabilityRepository
                .findByEventIdAndUserId(id, user.getId())
                .orElse(null);

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getCreatedBy(),
                event.getCreatedAt(),
                availability != null ? availability.getStatus() : null
        );
    }
}
