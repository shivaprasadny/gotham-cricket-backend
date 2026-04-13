package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
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

        matchRepository.save(match);

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
                        match.getCreatedBy()
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
                match.getCreatedBy()
        );
    }
}