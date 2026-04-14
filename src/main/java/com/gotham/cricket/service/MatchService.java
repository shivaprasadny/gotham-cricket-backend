package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.repository.AvailabilityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository; // ✅ ADD THIS
    private final NotificationService notificationService;
    private final AvailabilityRepository availabilityRepository;

    public String createMatch(String email, MatchRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Match match = new Match();
        match.setOpponentName(request.getOpponentName());
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue());
        match.setMatchType(request.getMatchType());
        match.setNotes(request.getNotes());
        match.setCreatedBy(user.getFullName());
        match.setStatus(request.getStatus() != null ? request.getStatus() : MatchStatus.UPCOMING);

        // ✅ SET TEAM (FIXED POSITION)
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            match.setTeam(team);
        }

        matchRepository.save(match);

        // ✅ SEND NOTIFICATION (CLEAN)
        notificationService.sendPushNotificationToUser(
                email,
                "Match Created",
                "Match vs " + request.getOpponentName() + " created."
        );

        return "Match created successfully";
    }

    public List<MatchResponse> getAllMatches(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return matchRepository.findAllByOrderByMatchDateAsc()
                .stream()
                .map(match -> {
                    Availability availability = availabilityRepository
                            .findByMatchIdAndUserId(match.getId(), user.getId())
                            .orElse(null);

                    return new MatchResponse(
                            match.getId(),
                            match.getOpponentName(),
                            match.getMatchDate(),
                            match.getVenue(),
                            match.getMatchType(),
                            match.getNotes(),
                            match.getCreatedBy(),
                            match.getStatus(),
                            match.getTeam() != null ? match.getTeam().getId() : null,
                            match.getTeam() != null ? match.getTeam().getTeamName() : null,
                            availability != null ? availability.getStatus() : null
                    );
                })
                .toList();
    }

    public MatchResponse getMatchById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        Availability availability = availabilityRepository
                .findByMatchIdAndUserId(match.getId(), user.getId())
                .orElse(null);

        return new MatchResponse(
                match.getId(),
                match.getOpponentName(),
                match.getMatchDate(),
                match.getVenue(),
                match.getMatchType(),
                match.getNotes(),
                match.getCreatedBy(),
                match.getStatus(),
                match.getTeam() != null ? match.getTeam().getId() : null,
                match.getTeam() != null ? match.getTeam().getTeamName() : null,
                availability != null ? availability.getStatus() : null
        );
    }

    public String updateMatch(Long id, MatchRequest request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        match.setOpponentName(request.getOpponentName());
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue());
        match.setMatchType(request.getMatchType());
        match.setNotes(request.getNotes());
        match.setStatus(request.getStatus() != null ? request.getStatus() : match.getStatus());

        // ✅ UPDATE TEAM ALSO
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            match.setTeam(team);
        } else {
            match.setTeam(null);
        }

        matchRepository.save(match);

        return "Match updated successfully";
    }
    @Transactional
    public String deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));


        availabilityRepository.deleteByMatchId(id);

        matchRepository.delete(match);

        return "Match deleted successfully";
    }
}