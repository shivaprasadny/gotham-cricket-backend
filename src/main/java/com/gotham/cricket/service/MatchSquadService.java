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
import jakarta.transaction.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class MatchSquadService {

    private final MatchRepository matchRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ChatRoomProvisioningService chatRoomProvisioningService;

    @Transactional
    public String addOrUpdateSquadMember(
            Long matchId,
            MatchSquadRequest request,
            String actorEmail
    ) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("A valid player must be selected");
        }

        Long replacingUserId = request.getReplacingUserId();
        if (replacingUserId != null && !replacingUserId.equals(request.getUserId())) {
            User replacedUser = userRepository.findById(replacingUserId)
                    .orElseThrow(() -> new RuntimeException("Player being replaced was not found"));
            MatchSquad replacedSquad = matchSquadRepository.findByMatchAndUser(match, replacedUser)
                    .orElseThrow(() -> new RuntimeException("Player being replaced is no longer in this squad"));
            matchSquadRepository.delete(replacedSquad);
            matchSquadRepository.flush();
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        MatchSquad existing = matchSquadRepository.findByMatchAndUser(match, user).orElse(null);
        Integer position = request.getSquadPosition();
        boolean isImpactPlayer = "IMPACT_PLAYER".equals(request.getRoleInMatch());
        boolean roleEligible = Boolean.TRUE.equals(request.getIsPlayingXi()) || isImpactPlayer;
        boolean isCaptain = roleEligible && Boolean.TRUE.equals(request.getIsCaptain());
        boolean isViceCaptain = roleEligible && Boolean.TRUE.equals(request.getIsViceCaptain());
        boolean isWicketKeeper = roleEligible && Boolean.TRUE.equals(request.getIsWicketKeeper());

        if (isCaptain && isViceCaptain) {
            throw new RuntimeException("The same player cannot be Captain and Vice Captain");
        }

        if (position != null) {
            if (Boolean.TRUE.equals(request.getIsPlayingXi()) && (position < 1 || position > 11)) {
                throw new RuntimeException("Playing XI position must be between 1 and 11");
            }
            if ("IMPACT_PLAYER".equals(request.getRoleInMatch()) && position != 12) {
                throw new RuntimeException("Impact Player must use position 12");
            }

            matchSquadRepository.findByMatchIdAndSquadPosition(matchId, position)
                    .filter(row -> existing == null || !row.getId().equals(existing.getId()))
                    .ifPresent(row -> {
                        throw new RuntimeException("That squad position is already occupied");
                    });
        }

        if (Boolean.TRUE.equals(request.getIsPlayingXi()) && existing == null) {
            long currentPlayingXiCount = matchSquadRepository.countByMatchAndIsPlayingXiTrue(match);
            if (currentPlayingXiCount >= 11) {
                throw new RuntimeException("Playing XI already has 11 players");
            }
        }

        if (Boolean.TRUE.equals(request.getIsPlayingXi())
                && existing != null
                && !Boolean.TRUE.equals(existing.getIsPlayingXi())) {
            long currentPlayingXiCount = matchSquadRepository.countByMatchAndIsPlayingXiTrue(match);
            if (currentPlayingXiCount >= 11) {
                throw new RuntimeException("Playing XI already has 11 players");
            }
        }

        MatchSquad squad = existing != null ? existing : new MatchSquad();

        // Each leadership/keeper role is unique across XI + Impact Player.
        // Captain/VC remain mutually exclusive on one player, while either may
        // also be wicketkeeper.
        List<MatchSquad> matchSquad = matchSquadRepository.findByMatch(match);
        for (MatchSquad row : matchSquad) {
            if (existing != null && row.getId().equals(existing.getId())) {
                continue;
            }
            if (isCaptain && isCaptain(row)) {
                row.setIsCaptain(false);
                if ("CAPTAIN".equals(row.getRoleInMatch())) row.setRoleInMatch(null);
            }
            if (isViceCaptain && isViceCaptain(row)) {
                row.setIsViceCaptain(false);
                if ("VICE_CAPTAIN".equals(row.getRoleInMatch())) row.setRoleInMatch(null);
            }
            if (isWicketKeeper && isWicketKeeper(row)) {
                row.setIsWicketKeeper(false);
                if ("WICKETKEEPER".equals(row.getRoleInMatch())) row.setRoleInMatch(null);
            }
        }
        matchSquadRepository.saveAll(matchSquad);

        squad.setMatch(match);
        squad.setUser(user);
        squad.setIsPlayingXi(request.getIsPlayingXi());
        squad.setRoleInMatch(request.getRoleInMatch());
        squad.setIsCaptain(isCaptain);
        squad.setIsViceCaptain(isViceCaptain);
        squad.setIsWicketKeeper(isWicketKeeper);
        squad.setSquadPosition(request.getSquadPosition());

        matchSquadRepository.save(squad);
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RuntimeException("Actor not found"));

        chatRoomProvisioningService.syncMatchRoomMembership(match, actor);

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
                            squad.getRoleInMatch(),
                            isCaptain(squad),
                            isViceCaptain(squad),
                            isWicketKeeper(squad),
                            squad.getSquadPosition()
                    );
                })
                .sorted(java.util.Comparator.comparing(
                        MatchSquadResponse::getSquadPosition,
                        java.util.Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();
    }

    @Transactional
    public String removeSquadMember(
            Long matchId,
            Long userId,
            String actorEmail
    ) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        MatchSquad squad = matchSquadRepository.findByMatchAndUser(match, user)
                .orElseThrow(() -> new RuntimeException("User is not part of this squad"));

        matchSquadRepository.delete(squad);
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RuntimeException("Actor not found"));

        chatRoomProvisioningService.syncMatchRoomMembership(match, actor);

        return "Squad member removed successfully";
    }

    private boolean isCaptain(MatchSquad squad) {
        return Boolean.TRUE.equals(squad.getIsCaptain())
                || "CAPTAIN".equals(squad.getRoleInMatch());
    }

    private boolean isViceCaptain(MatchSquad squad) {
        return Boolean.TRUE.equals(squad.getIsViceCaptain())
                || "VICE_CAPTAIN".equals(squad.getRoleInMatch());
    }

    private boolean isWicketKeeper(MatchSquad squad) {
        return Boolean.TRUE.equals(squad.getIsWicketKeeper())
                || "WICKETKEEPER".equals(squad.getRoleInMatch());
    }
}
