package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchSquadRequest;
import com.gotham.cricket.dto.MatchSquadResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.MatchSquad;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.MatchSquadRepository;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchSquadService {

    private final MatchRepository matchRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public String addOrUpdateSquadMember(Long matchId, MatchSquadRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        MatchSquad existing = matchSquadRepository.findByMatchAndUser(match, user).orElse(null);

        if (request.getIsPlayingXi() && existing == null) {
            long currentPlayingXiCount = matchSquadRepository.countByMatchAndIsPlayingXiTrue(match);
            if (currentPlayingXiCount >= 11) {
                throw new RuntimeException("Playing XI already has 11 players");
            }
        }

        if (request.getIsPlayingXi() && existing != null && !existing.getIsPlayingXi()) {
            long currentPlayingXiCount = matchSquadRepository.countByMatchAndIsPlayingXiTrue(match);
            if (currentPlayingXiCount >= 11) {
                throw new RuntimeException("Playing XI already has 11 players");
            }
        }

        MatchSquad squad = existing != null ? existing : new MatchSquad();
        squad.setMatch(match);
        squad.setUser(user);
        squad.setIsPlayingXi(request.getIsPlayingXi());
        squad.setRoleInMatch(request.getRoleInMatch());

        matchSquadRepository.save(squad);

        return "Squad member saved successfully";
    }

    public List<MatchSquadResponse> getSquadByMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        return matchSquadRepository.findByMatch(match)
                .stream()
                .map(squad -> {
                    User user = squad.getUser();
                    MemberProfile profile = memberProfileRepository.findByUser(user).orElse(null);

                    return new MatchSquadResponse(
                            squad.getId(),
                            user.getId(),
                            user.getFullName(),
                            profile != null ? profile.getNickname() : null,
                            profile != null ? profile.getPlayerType() : null,
                            profile != null ? profile.getJerseyNumber() : null,
                            squad.getIsPlayingXi(),
                            squad.getRoleInMatch()
                    );
                })
                .toList();
    }

    public String removeSquadMember(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        MatchSquad squad = matchSquadRepository.findByMatchAndUser(match, user)
                .orElseThrow(() -> new RuntimeException("User is not part of this squad"));

        matchSquadRepository.delete(squad);

        return "Squad member removed successfully";
    }
}