package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.entity.League;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.repository.*;
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

        // Home team is required in most cases
        if (request.getHomeTeamId() != null) {
            homeTeam = teamRepository.findById(request.getHomeTeamId())
                    .orElseThrow(() -> new RuntimeException("Home team not found"));
        }

        // Away team optional for intra-club / club-vs-club
        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepository.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));
        }

        // Optional league link
        if (request.getLeagueId() != null) {
            league = leagueRepository.findById(request.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
        }

        // Prevent same team vs same team
        if (homeTeam != null && awayTeam != null && homeTeam.getId().equals(awayTeam.getId())) {
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
        match.setVenue(request.getVenue());
        match.setMatchType(request.getMatchType());
        match.setNotes(request.getNotes());
        match.setCreatedBy(user.getFullName());
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
                            match.getNotes(),
                            match.getCreatedBy(),
                            match.getStatus(),
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
                match.getNotes(),
                match.getCreatedBy(),
                match.getStatus(),
                availability != null ? availability.getStatus() : null
        );
    }


    // Update an existing match
    public String updateMatch(Long id, MatchRequest request) {

        // Find the match first
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Team homeTeam = null;
        Team awayTeam = null;
        League league = null;


        if (request.getHomeTeamId() == null) {
            throw new RuntimeException("Home team is required");
        }



        // Load home team if sent
        if (request.getHomeTeamId() != null) {
            homeTeam = teamRepository.findById(request.getHomeTeamId())
                    .orElseThrow(() -> new RuntimeException("Home team not found"));
        }

        // Load away team if sent
        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepository.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));
        }

        // Load league if sent
        if (request.getLeagueId() != null) {
            league = leagueRepository.findById(request.getLeagueId())
                    .orElseThrow(() -> new RuntimeException("League not found"));
        }

        // Prevent same team vs same team
        if (homeTeam != null && awayTeam != null && homeTeam.getId().equals(awayTeam.getId())) {
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

        // Update fields
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setExternalOpponentName(
                request.getExternalOpponentName() != null
                        ? request.getExternalOpponentName().trim()
                        : null
        );
        match.setLeague(league);
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue());
        match.setMatchType(request.getMatchType());
        match.setNotes(request.getNotes());

        // Keep current status if request status is null
        if (request.getStatus() != null) {
            match.setStatus(request.getStatus());
        }

        matchRepository.save(match);

        return "Match updated successfully";
    }

    // Delete a match safely
    @Transactional
    public String deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // First delete squad rows linked to this match
        matchSquadRepository.deleteByMatchId(id);

        // Then delete availability rows linked to this match
        availabilityRepository.deleteByMatchId(id);

        // Finally delete the match itself
        matchRepository.delete(match);

        return "Match deleted successfully";
    }
    // Return future matches only
    public List<Match> getUpcomingMatches() {
        return matchRepository.findByMatchDateAfter(LocalDateTime.now());
    }
}