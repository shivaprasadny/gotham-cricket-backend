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

    // Create match using flexible team/opponent structure
    public String createMatch(String email, MatchRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Team homeTeam = null;
        Team awayTeam = null;
        League league = null;

        if (request.getHomeTeamId() == null) {
            throw new RuntimeException("Home team is required");
        }

        // Home team required
        homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found"));

        // Away team optional
        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepository.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));
        }

        // League optional
        if (request.getLeagueId() != null) {
            league = leagueRepository.findById(request.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
        }

        // Prevent same team vs same team
        if (awayTeam != null && homeTeam.getId().equals(awayTeam.getId())) {
            throw new RuntimeException("Home team and away team cannot be the same");
        }

        // Validate opponent setup
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

        if (request.getMatchType() == null || request.getMatchType().trim().isEmpty()) {
            throw new RuntimeException("Match type is required");
        }

        if (request.getMatchFormat() == null || request.getMatchFormat().trim().isEmpty()) {
            throw new RuntimeException("Match format is required");
        }

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
        match.setMatchType(request.getMatchType().trim());
        match.setMatchFormat(request.getMatchFormat().trim());
        match.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        match.setCreatedBy(user.getFullName());
        match.setMatchFee(request.getMatchFee());
        match.setStatus(request.getStatus() != null ? request.getStatus() : MatchStatus.UPCOMING);

        matchRepository.save(match);

        return "Match created successfully";
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
                            match.getMatchFormat(),
                            match.getNotes(),
                            match.getCreatedBy(),
                            match.getStatus(),
                            match.getMatchFee(),
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
                match.getMatchFormat(),
                match.getNotes(),
                match.getCreatedBy(),
                match.getStatus(),
                match.getMatchFee(),
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

        if (request.getMatchType() == null || request.getMatchType().trim().isEmpty()) {
            throw new RuntimeException("Match type is required");
        }

        if (request.getMatchFormat() == null || request.getMatchFormat().trim().isEmpty()) {
            throw new RuntimeException("Match format is required");
        }

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
        match.setMatchType(request.getMatchType().trim());
        match.setMatchFormat(request.getMatchFormat().trim());
        match.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        match.setMatchFee(request.getMatchFee());

        if (request.getStatus() != null) {
            match.setStatus(request.getStatus());
        }

        matchRepository.save(match);

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
}