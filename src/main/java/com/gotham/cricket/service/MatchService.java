package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.entity.League;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.repository.AvailabilityRepository;
import com.gotham.cricket.repository.LeagueRepository;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.MatchSquadRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final AvailabilityRepository availabilityRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final NotificationService notificationService;
    private final ChatRoomProvisioningService chatRoomProvisioningService;

    // Create match using flexible team/opponent structure
    public MatchResponse createMatch(String email, MatchRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Team homeTeam;
        Team awayTeam = null;
        League league = null;

        if (request.getHomeTeamId() == null) {
            throw new RuntimeException("Home team is required");
        }

        homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found"));

        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepository.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));
        }

        if (request.getLeagueId() != null) {
            league = leagueRepository.findById(request.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
        }

        if (awayTeam != null && homeTeam.getId().equals(awayTeam.getId())) {
            throw new RuntimeException("Home team and away team cannot be the same");
        }

        boolean hasAwayTeam = awayTeam != null;
        boolean hasExternalOpponent =
                request.getExternalOpponentName() != null &&
                        !request.getExternalOpponentName().trim().isEmpty();

        if (!hasAwayTeam && !hasExternalOpponent) {
            throw new RuntimeException("Please select an away team or enter an external opponent name");
        }

        if (request.getMatchDate() == null) {
            throw new RuntimeException("Match date is required");
        }

        if (request.getVenue() == null || request.getVenue().trim().isEmpty()) {
            throw new RuntimeException("Venue is required");
        }

        if (request.getMatchFormat() == null || request.getMatchFormat().trim().isEmpty()) {
            throw new RuntimeException("Match format is required");
        }

        String homeAway = normalizeHomeAway(request.getHomeAway());

        Match match = new Match();
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setExternalOpponentName(
                request.getExternalOpponentName() != null
                        ? request.getExternalOpponentName().trim()
                        : null
        );
        match.setLeague(league);
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue().trim());
        // Keep the legacy required column populated without exposing it in UI.
        match.setMatchType(hasText(request.getMatchType()) ? request.getMatchType().trim() : "MATCH");
        match.setHomeAway(homeAway);
        match.setMatchFormat(request.getMatchFormat().trim());
        match.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        match.setCreatedBy(user.getFullName());
        match.setMatchFee(request.getMatchFee());
        match.setMatchFeeAmount(request.getMatchFeeAmount());
        match.setMatchFeeDueDate(request.getMatchFeeDueDate());
        match.setMatchFeeDescription(request.getMatchFeeDescription());
        match.setStatus(request.getStatus() != null ? request.getStatus() : MatchStatus.UPCOMING);

        // save match first
        Match savedMatch = matchRepository.save(match);
        chatRoomProvisioningService.ensureMatchRoom(savedMatch);

        // build opponent text
        String opponentName = awayTeam != null
                ? awayTeam.getTeamName()
                : savedMatch.getExternalOpponentName();

        // create notification for all users
        notificationService.createForAllUsers(
                "New Match Added",
                homeTeam.getTeamName() + " vs " + opponentName,
                "MATCH",
                "MatchDetails",
                savedMatch.getId()
        );

        return new MatchResponse(
                savedMatch.getId(),
                savedMatch.getHomeTeam() != null ? savedMatch.getHomeTeam().getId() : null,
                savedMatch.getHomeTeam() != null ? savedMatch.getHomeTeam().getTeamName() : null,
                savedMatch.getAwayTeam() != null ? savedMatch.getAwayTeam().getId() : null,
                savedMatch.getAwayTeam() != null ? savedMatch.getAwayTeam().getTeamName() : null,
                savedMatch.getExternalOpponentName(),
                savedMatch.getLeague() != null ? savedMatch.getLeague().getId() : null,
                savedMatch.getLeague() != null ? savedMatch.getLeague().getName() : null,
                savedMatch.getMatchDate(),
                savedMatch.getVenue(),
                savedMatch.getMatchType(),
                homeAwayOrDefault(savedMatch),
                savedMatch.getMatchFormat(),
                savedMatch.getNotes(),
                savedMatch.getCreatedBy(),
                savedMatch.getStatus(),
                savedMatch.getMatchFee(),
                savedMatch.getMatchFeeAmount(),
                savedMatch.getMatchFeeDueDate(),
                savedMatch.getMatchFeeDescription(),
                null
        );
    }

    // Return all matches with automatic old match completion
    public List<MatchResponse> getAllMatches(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<Match> matches = matchRepository.findAllByOrderByMatchDateAsc();

        // Auto-mark old upcoming matches as completed
        for (Match match : matches) {
            if (match.getMatchDate() != null &&
                    match.getMatchDate().isBefore(LocalDateTime.now()) &&
                    match.getStatus() == MatchStatus.UPCOMING) {

                match.setStatus(MatchStatus.COMPLETED);
            }
        }

        matchRepository.saveAll(matches);

        return matches.stream()
                .map(match -> {
                    Availability availability = availabilityRepository
                            .findByMatchIdAndUserId(match.getId(), user.getId())
                            .orElse(null);

                    return new MatchResponse(
                            match.getId(),
                            match.getHomeTeam() != null ? match.getHomeTeam().getId() : null,
                            match.getHomeTeam() != null ? match.getHomeTeam().getTeamName() : null,
                            match.getAwayTeam() != null ? match.getAwayTeam().getId() : null,
                            match.getAwayTeam() != null ? match.getAwayTeam().getTeamName() : null,
                            match.getExternalOpponentName(),
                            match.getLeague() != null ? match.getLeague().getId() : null,
                            match.getLeague() != null ? match.getLeague().getName() : null,
                            match.getMatchDate(),
                            match.getVenue(),
                            match.getMatchType(),
                            homeAwayOrDefault(match),
                            match.getMatchFormat(),
                            match.getNotes(),
                            match.getCreatedBy(),
                            match.getStatus(),
                            match.getMatchFee(),
                            match.getMatchFeeAmount(),
                            match.getMatchFeeDueDate(),
                            match.getMatchFeeDescription(),
                            availability != null ? availability.getStatus() : null
                    );
                })
                .toList();
    }

    // Get one match
    public MatchResponse getMatchById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Availability availability = availabilityRepository
                .findByMatchIdAndUserId(match.getId(), user.getId())
                .orElse(null);

        return new MatchResponse(
                match.getId(),
                match.getHomeTeam() != null ? match.getHomeTeam().getId() : null,
                match.getHomeTeam() != null ? match.getHomeTeam().getTeamName() : null,
                match.getAwayTeam() != null ? match.getAwayTeam().getId() : null,
                match.getAwayTeam() != null ? match.getAwayTeam().getTeamName() : null,
                match.getExternalOpponentName(),
                match.getLeague() != null ? match.getLeague().getId() : null,
                match.getLeague() != null ? match.getLeague().getName() : null,
                match.getMatchDate(),
                match.getVenue(),
                match.getMatchType(),
                homeAwayOrDefault(match),
                match.getMatchFormat(),
                match.getNotes(),
                match.getCreatedBy(),
                match.getStatus(),
                match.getMatchFee(),
                match.getMatchFeeAmount(),
                match.getMatchFeeDueDate(),
                match.getMatchFeeDescription(),
                availability != null ? availability.getStatus() : null
        );
    }

    // Update existing match
    public String updateMatch(Long id, MatchRequest request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Team homeTeam = null;
        Team awayTeam = null;
        League league = null;

        if (request.getHomeTeamId() == null) {
            throw new RuntimeException("Home team is required");
        }

        homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found"));

        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepository.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));
        }

        if (request.getLeagueId() != null) {
            league = leagueRepository.findById(request.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
        }

        if (awayTeam != null && homeTeam.getId().equals(awayTeam.getId())) {
            throw new RuntimeException("Home team and away team cannot be the same");
        }

        boolean hasAwayTeam = awayTeam != null;
        boolean hasExternalOpponent =
                request.getExternalOpponentName() != null &&
                        !request.getExternalOpponentName().trim().isEmpty();

        if (!hasAwayTeam && !hasExternalOpponent) {
            throw new RuntimeException("Please select an away team or enter an external opponent name");
        }

        if (request.getMatchDate() == null) {
            throw new RuntimeException("Match date is required");
        }

        if (request.getVenue() == null || request.getVenue().trim().isEmpty()) {
            throw new RuntimeException("Venue is required");
        }

        if (request.getMatchFormat() == null || request.getMatchFormat().trim().isEmpty()) {
            throw new RuntimeException("Match format is required");
        }

        String homeAway = normalizeHomeAway(request.getHomeAway());

        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setExternalOpponentName(
                request.getExternalOpponentName() != null
                        ? request.getExternalOpponentName().trim()
                        : null
        );
        match.setLeague(league);
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue().trim());
        if (hasText(request.getMatchType())) {
            match.setMatchType(request.getMatchType().trim());
        } else if (!hasText(match.getMatchType())) {
            match.setMatchType("MATCH");
        }
        match.setHomeAway(homeAway);
        match.setMatchFormat(request.getMatchFormat().trim());
        match.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        match.setMatchFee(request.getMatchFee());
        match.setMatchFeeAmount(request.getMatchFeeAmount());
        match.setMatchFeeDueDate(request.getMatchFeeDueDate());
        match.setMatchFeeDescription(request.getMatchFeeDescription());

        if (request.getStatus() != null) {
            match.setStatus(request.getStatus());
        }

        Match savedMatch = matchRepository.save(match);

        String opponentName = awayTeam != null
                ? awayTeam.getTeamName()
                : savedMatch.getExternalOpponentName();

        notificationService.createForAllUsers(
                "Match Updated",
                homeTeam.getTeamName() + " vs " + opponentName,
                "MATCH",
                "MatchDetails",
                savedMatch.getId()
        );

        return "Match updated successfully";
    }

    // Delete match safely
    @Transactional
    public String deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        matchSquadRepository.deleteByMatchId(id);
        availabilityRepository.deleteByMatchId(id);
        matchRepository.delete(match);

        return "Match deleted successfully";
    }

    // Return future matches only
    public List<Match> getUpcomingMatches() {
        return matchRepository.findByMatchDateAfter(LocalDateTime.now());
    }

    private String normalizeHomeAway(String value) {
        String normalized = hasText(value) ? value.trim().toUpperCase() : "HOME";
        if (!"HOME".equals(normalized) && !"AWAY".equals(normalized)) {
            throw new RuntimeException("Home/Away must be HOME or AWAY");
        }
        return normalized;
    }

    private String homeAwayOrDefault(Match match) {
        return hasText(match.getHomeAway()) ? match.getHomeAway().toUpperCase() : "HOME";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
