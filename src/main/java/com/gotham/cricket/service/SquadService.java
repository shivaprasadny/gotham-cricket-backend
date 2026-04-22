package com.gotham.cricket.service;

import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Squad;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.SquadRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    /**
     * Add or update player in squad
     */
    public String addPlayer(Long matchId, Long userId, boolean playingXI) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if already exists (update instead of duplicate)
        Squad squad = squadRepository.findByMatchAndUser(match, user)
                .orElse(new Squad());

        squad.setMatch(match);
        squad.setUser(user);
        squad.setPlayingXi(playingXI);

        squadRepository.save(squad);

        return "Player added/updated in squad";
    }

    /**
     * Get squad for a match
     */
    public List<Squad> getSquad(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        return squadRepository.findByMatch(match);
    }

    /**
     * Remove player from squad
     */
    public String removePlayer(Long matchId, Long userId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Squad squad = squadRepository.findByMatchAndUser(match, user)
                .orElseThrow(() -> new RuntimeException("Player not in squad"));

        squadRepository.delete(squad);

        return "Player removed from squad";
    }
}