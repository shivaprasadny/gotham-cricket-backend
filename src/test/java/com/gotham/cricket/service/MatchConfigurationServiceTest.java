package com.gotham.cricket.service;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchSquadRequest;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MatchConfigurationServiceTest {

    @Autowired private MatchService matchService;
    @Autowired private MatchSquadService matchSquadService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void createMatchPersistsAwaySelection() {
        User admin = saveUser("Home Away Admin", Role.ADMIN);
        Team team = saveTeam("Home Away Team");

        MatchRequest request = new MatchRequest();
        request.setHomeTeamId(team.getId());
        request.setExternalOpponentName("External XI");
        request.setMatchDate(LocalDateTime.now().plusDays(1));
        request.setVenue("Away Ground");
        request.setHomeAway("AWAY");
        request.setMatchFormat("T20");
        request.setStatus(MatchStatus.UPCOMING);

        var response = matchService.createMatch(admin.getEmail(), request);

        assertEquals("AWAY", response.getHomeAway());
        assertEquals("AWAY", matchRepository.findById(response.getId()).orElseThrow().getHomeAway());
    }

    @Test
    void captainOrViceCaptainMayAlsoKeepWicketButCannotHoldBothLeadershipRoles() {
        Match match = saveMatch();
        User player = saveUser("Role Player", Role.PLAYER);

        MatchSquadRequest captainKeeper = squadRequest(player, true, false, true);
        matchSquadService.addOrUpdateSquadMember(
                match.getId(),
                captainKeeper,
                "test@test.com"
        );

        var saved = matchSquadService.getSquadByMatch(match.getId()).getFirst();
        assertTrue(saved.getIsCaptain());
        assertFalse(saved.getIsViceCaptain());
        assertTrue(saved.getIsWicketKeeper());

        MatchSquadRequest invalid = squadRequest(player, true, true, true);
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> matchSquadService.addOrUpdateSquadMember(
                        match.getId(),
                        invalid,
                        "test@test.com"));
        assertEquals("The same player cannot be Captain and Vice Captain", exception.getMessage());
    }

    @Test
    void impactPlayerSupportsViceCaptainAndWicketKeeperRoles() {
        Match match = saveMatch();
        User player = saveUser("Impact Player", Role.PLAYER);

        MatchSquadRequest request = squadRequest(player, false, true, true);
        request.setIsPlayingXi(false);
        request.setRoleInMatch("IMPACT_PLAYER");
        request.setSquadPosition(12);
        matchSquadService.addOrUpdateSquadMember(
                match.getId(),
                request,
                "test@test.com"
        );

        var saved = matchSquadService.getSquadByMatch(match.getId()).getFirst();
        assertEquals("IMPACT_PLAYER", saved.getRoleInMatch());
        assertTrue(saved.getIsViceCaptain());
        assertTrue(saved.getIsWicketKeeper());
    }

    private MatchSquadRequest squadRequest(
            User player,
            boolean captain,
            boolean viceCaptain,
            boolean wicketKeeper
    ) {
        MatchSquadRequest request = new MatchSquadRequest();
        request.setUserId(player.getId());
        request.setIsPlayingXi(true);
        request.setSquadPosition(1);
        request.setIsCaptain(captain);
        request.setIsViceCaptain(viceCaptain);
        request.setIsWicketKeeper(wicketKeeper);
        return request;
    }

    private Match saveMatch() {
        Match match = new Match();
        match.setHomeTeam(saveTeam("Squad Role Team " + System.nanoTime()));
        match.setExternalOpponentName("Opponent");
        match.setMatchDate(LocalDateTime.now().plusDays(1));
        match.setVenue("Gotham Ground");
        match.setHomeAway("HOME");
        match.setMatchType("MATCH");
        match.setMatchFormat("T20");
        match.setCreatedBy("test");
        match.setStatus(MatchStatus.UPCOMING);
        return matchRepository.save(match);
    }

    private Team saveTeam(String name) {
        Team team = new Team();
        team.setTeamName(name);
        return teamRepository.save(team);
    }

    private User saveUser(String name, Role role) {
        User user = new User();
        user.setFirstName(name);
        user.setLastName("Test");
        user.setEmail(name.toLowerCase().replace(" ", ".") + "." + System.nanoTime() + "@gotham.test");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setStatus(UserStatus.APPROVED);
        return userRepository.save(user);
    }
}
