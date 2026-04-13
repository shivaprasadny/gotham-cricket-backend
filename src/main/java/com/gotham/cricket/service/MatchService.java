package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

        matchRepository.save(match);
        notificationService.sendPushNotificationToUser(
                email,
                "Match Created",
                "Your match against " + request.getOpponentName() + " was created."
        );

        return "Match created successfully";
    }

    public List<MatchResponse> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateAsc()
                .stream()
                .map(match -> new MatchResponse(
                        match.getId(),
                        match.getOpponentName(),
                        match.getMatchDate(),
                        match.getVenue(),
                        match.getMatchType(),
                        match.getNotes(),
                        match.getCreatedBy(),
                        match.getStatus()
                ))
                .toList();
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        return new MatchResponse(
                match.getId(),
                match.getOpponentName(),
                match.getMatchDate(),
                match.getVenue(),
                match.getMatchType(),
                match.getNotes(),
                match.getCreatedBy(),
                match.getStatus()
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

        matchRepository.save(match);

        return "Match updated successfully";
    }

    public String deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        matchRepository.delete(match);

        return "Match deleted successfully";
    }
}