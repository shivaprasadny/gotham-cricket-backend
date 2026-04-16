package com.gotham.cricket.service;

import com.gotham.cricket.dto.AvailabilityRequest;
import com.gotham.cricket.dto.AvailabilityResponse;
import com.gotham.cricket.dto.AvailabilitySummaryResponse;
import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.AvailabilityStatus;
import com.gotham.cricket.repository.AvailabilityRepository;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    // ✅ MARK AVAILABILITY
    public String markAvailability(String email, AvailabilityRequest request) {

        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + request.getMatchId()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Availability availability = availabilityRepository
                .findByMatchIdAndUserId(match.getId(), user.getId())
                .orElse(new Availability());

        availability.setMatch(match);
        availability.setUser(user);
        availability.setStatus(request.getStatus());
        availability.setMessage(request.getMessage());

        availabilityRepository.save(availability);

        return "Availability saved successfully";
    }

    // ✅ GET ALL AVAILABILITY FOR MATCH
    public List<AvailabilityResponse> getAvailabilityByMatch(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        return availabilityRepository.findByMatch(match)
                .stream()
                .map(a -> new AvailabilityResponse(
                        a.getId(),
                        a.getMatch().getId(),
                        a.getUser().getId(),
                        a.getUser().getFullName(),
                        a.getStatus(),
                        a.getMessage()
                ))
                .toList();
    }

    // ✅ SUMMARY (COUNTS)
    public AvailabilitySummaryResponse getAvailabilitySummary(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        long availableCount = availabilityRepository.countByMatchAndStatus(match, AvailabilityStatus.AVAILABLE);
        long maybeCount = availabilityRepository.countByMatchAndStatus(match, AvailabilityStatus.MAYBE);
        long notAvailableCount = availabilityRepository.countByMatchAndStatus(match, AvailabilityStatus.NOT_AVAILABLE);
        long injuredCount = availabilityRepository.countByMatchAndStatus(match, AvailabilityStatus.INJURED);

        long totalResponses = availableCount + maybeCount + notAvailableCount + injuredCount;

        return new AvailabilitySummaryResponse(
                matchId,
                availableCount,
                maybeCount,
                notAvailableCount,
                injuredCount,
                totalResponses
        );
    }

    public String submitAvailability(Long matchId, String email, AvailabilityRequest request) {

        // Find logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find match
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // 🔒 LOCK RULE:
        // If current time is after match time, block availability update
        if (match.getMatchDate() != null && match.getMatchDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Availability is locked because the match time has passed");
        }

        // Check if user already submitted availability for this match
        Availability availability = availabilityRepository
                .findByMatchIdAndUserId(matchId, user.getId())
                .orElse(new Availability());

        // Set values
        availability.setMatch(match);
        availability.setUser(user);
        availability.setStatus(request.getStatus());
        availability.setMessage(request.getMessage());

        // Save
        availabilityRepository.save(availability);

        return "Availability submitted successfully";
    }
}