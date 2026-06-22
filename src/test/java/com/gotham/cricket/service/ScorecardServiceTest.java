package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingEntryRequest;
import com.gotham.cricket.dto.scorecard.BowlingEntryRequest;
import com.gotham.cricket.dto.scorecard.SaveInningsRequest;
import com.gotham.cricket.dto.scorecard.SaveScorecardRequest;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.MatchScorecard;
import com.gotham.cricket.entity.MatchSquad;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.exception.ScorecardAlreadyExistsException;
import com.gotham.cricket.exception.ScorecardValidationException;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.MatchScorecardRepository;
import com.gotham.cricket.repository.MatchSquadRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ScorecardServiceTest {

    @Autowired private ScorecardService scorecardService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private MatchScorecardRepository matchScorecardRepository;
    @Autowired private MatchSquadRepository matchSquadRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StatisticsService statisticsService;

    @Test
    void createDraftPreventsDuplicateScorecard() {
        Match match = saveMatch();
        scorecardService.createDraft(match.getId(), minimalRequest(), "admin@gotham.com");

        assertThrows(ScorecardAlreadyExistsException.class,
                () -> scorecardService.createDraft(match.getId(), minimalRequest(), "admin@gotham.com"));
    }

    @Test
    void createDraftStoresDraftScorecard() {
        Match match = saveMatch();

        var response = scorecardService.createDraft(match.getId(), minimalRequest(), "admin@gotham.com");

        assertEquals(ScorecardStatus.DRAFT, response.getStatus());
        MatchScorecard scorecard = matchScorecardRepository.findByMatchId(match.getId()).orElseThrow();
        assertEquals(ScorecardStatus.DRAFT, scorecard.getStatus());
    }

    @Test
    void publishValidScorecardMarksMatchCompleted() {
        Match match = saveMatch();
        scorecardService.createDraft(match.getId(), minimalRequest(), "admin@gotham.com");

        var response = scorecardService.publishScorecard(match.getId(), "admin@gotham.com");

        assertEquals(ScorecardStatus.PUBLISHED, response.getStatus());
        assertEquals(MatchStatus.COMPLETED, matchRepository.findById(match.getId()).orElseThrow().getStatus());
    }

    @Test
    void approvedPlayerInMatchSquadCanBeSaved() {
        Match match = saveMatch();
        User player = saveUser(UserStatus.APPROVED, "Squad", "Player");
        saveMatchSquad(match, player);

        var response = scorecardService.createDraft(
                match.getId(),
                requestWithBatting(battingEntry(player.getId(), null, 1)),
                "admin@gotham.com"
        );

        assertEquals(player.getId(), response.getInnings().getFirst().getBatting().getFirst().getPlayerId());
    }

    @Test
    void approvedPlayerOutsideMatchSquadCanBeSaved() {
        Match match = saveMatch();
        User squadPlayer = saveUser(UserStatus.APPROVED, "Squad", "Member");
        User replacement = saveUser(UserStatus.APPROVED, "Late", "Replacement");
        saveMatchSquad(match, squadPlayer);

        var response = scorecardService.createDraft(
                match.getId(),
                requestWithBatting(battingEntry(replacement.getId(), null, 1)),
                "captain@gotham.com"
        );

        assertEquals(replacement.getId(), response.getInnings().getFirst().getBatting().getFirst().getPlayerId());
    }

    @Test
    void outsideSquadBattingPerformanceCanBePublished() {
        Match match = saveMatch();
        User squadPlayer = saveUser(UserStatus.APPROVED, "Original", "Player");
        User replacement = saveUser(UserStatus.APPROVED, "Batting", "Replacement");
        saveMatchSquad(match, squadPlayer);
        scorecardService.createDraft(
                match.getId(),
                requestWithBatting(battingEntry(replacement.getId(), null, 1)),
                "admin@gotham.com"
        );

        var response = scorecardService.publishScorecard(match.getId(), "admin@gotham.com");

        assertEquals(ScorecardStatus.PUBLISHED, response.getStatus());
        assertEquals(replacement.getId(), response.getInnings().getFirst().getBatting().getFirst().getPlayerId());
    }

    @Test
    void outsideSquadBowlingPerformanceCanBePublished() {
        Match match = saveMatch();
        User squadPlayer = saveUser(UserStatus.APPROVED, "Original", "Bowler");
        User replacement = saveUser(UserStatus.APPROVED, "Bowling", "Replacement");
        saveMatchSquad(match, squadPlayer);
        scorecardService.createDraft(
                match.getId(),
                requestWithBowling(bowlingEntry(replacement.getId(), null)),
                "captain@gotham.com"
        );

        var response = scorecardService.publishScorecard(match.getId(), "captain@gotham.com");

        assertEquals(ScorecardStatus.PUBLISHED, response.getStatus());
        assertEquals(replacement.getId(), response.getInnings().getFirst().getBowling().getFirst().getPlayerId());
    }

    @Test
    void publishedOutsideSquadPerformanceCountsTowardPlayerStatistics() {
        Match match = saveMatch();
        User squadPlayer = saveUser(UserStatus.APPROVED, "Original", "Member");
        User replacement = saveUser(UserStatus.APPROVED, "Stats", "Replacement");
        saveMatchSquad(match, squadPlayer);

        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBattingEntries(List.of(battingEntry(replacement.getId(), null, 1)));
        request.getInnings().getFirst().setBowlingEntries(List.of(bowlingEntry(replacement.getId(), null)));
        scorecardService.createDraft(match.getId(), request, "admin@gotham.com");
        scorecardService.publishScorecard(match.getId(), "admin@gotham.com");

        var statistics = statisticsService.getPlayerStatistics(replacement.getId(), null);

        assertEquals(1, statistics.getMatches());
        assertEquals(32, statistics.getTotalRuns());
        assertEquals(2, statistics.getWickets());
        assertEquals(18, statistics.getTotalLegalBalls());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "PENDING", "REJECTED", "EMAIL_PENDING"})
    void nonApprovedInternalPlayersAreRejected(UserStatus status) {
        Match match = saveMatch();
        User player = saveUser(status, status.name(), "Player");

        ScorecardValidationException exception = assertThrows(
                ScorecardValidationException.class,
                () -> scorecardService.createDraft(
                        match.getId(),
                        requestWithBatting(battingEntry(player.getId(), null, 1)),
                        "admin@gotham.com"
                )
        );

        assertEquals("User must be approved", exception.getMessage());
    }

    @Test
    void missingInternalPlayerIsRejected() {
        Match match = saveMatch();

        ScorecardValidationException exception = assertThrows(
                ScorecardValidationException.class,
                () -> scorecardService.createDraft(
                        match.getId(),
                        requestWithBatting(battingEntry(Long.MAX_VALUE, null, 1)),
                        "admin@gotham.com"
                )
        );

        assertEquals("User not found with id: " + Long.MAX_VALUE, exception.getMessage());
    }

    @Test
    void duplicateBattingPlayersRemainRejected() {
        Match match = saveMatch();
        User player = saveUser(UserStatus.APPROVED, "Duplicate", "Batter");
        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBattingEntries(List.of(
                battingEntry(player.getId(), null, 1),
                battingEntry(player.getId(), null, 2)
        ));

        ScorecardValidationException exception = assertThrows(
                ScorecardValidationException.class,
                () -> scorecardService.createDraft(match.getId(), request, "admin@gotham.com")
        );

        assertTrue(exception.getMessage().startsWith("Duplicate batting player"));
    }

    @Test
    void duplicateBowlingPlayersRemainRejected() {
        Match match = saveMatch();
        User player = saveUser(UserStatus.APPROVED, "Duplicate", "Bowler");
        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBowlingEntries(List.of(
                bowlingEntry(player.getId(), null),
                bowlingEntry(player.getId(), null)
        ));

        ScorecardValidationException exception = assertThrows(
                ScorecardValidationException.class,
                () -> scorecardService.createDraft(match.getId(), request, "admin@gotham.com")
        );

        assertTrue(exception.getMessage().startsWith("Duplicate bowling player"));
    }

    @Test
    void externalPlayerNamesStillWork() {
        Match match = saveMatch();
        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBattingEntries(List.of(battingEntry(null, "External Batter", 1)));
        request.getInnings().getFirst().setBowlingEntries(List.of(bowlingEntry(null, "External Bowler")));

        var response = scorecardService.createDraft(match.getId(), request, "admin@gotham.com");

        assertNull(response.getInnings().getFirst().getBatting().getFirst().getPlayerId());
        assertEquals("External Batter", response.getInnings().getFirst().getBatting().getFirst().getPlayerName());
        assertNull(response.getInnings().getFirst().getBowling().getFirst().getPlayerId());
        assertEquals("External Bowler", response.getInnings().getFirst().getBowling().getFirst().getPlayerName());
    }

    @Test
    void providingInternalIdAndExternalNameRemainsRejected() {
        Match match = saveMatch();
        User player = saveUser(UserStatus.APPROVED, "Internal", "Player");

        ScorecardValidationException exception = assertThrows(
                ScorecardValidationException.class,
                () -> scorecardService.createDraft(
                        match.getId(),
                        requestWithBatting(battingEntry(player.getId(), "External Player", 1)),
                        "admin@gotham.com"
                )
        );

        assertEquals(
                "Each batting entry must have exactly one of internal player or external player name",
                exception.getMessage()
        );
    }

    private Match saveMatch() {
        Team home = new Team();
        home.setTeamName("Gotham Knights");
        home = teamRepository.save(home);

        Match match = new Match();
        match.setHomeTeam(home);
        match.setMatchDate(LocalDateTime.now().plusDays(1));
        match.setVenue("Central Park");
        match.setMatchType("League");
        match.setMatchFormat("T20");
        match.setCreatedBy("admin@gotham.com");
        match.setStatus(MatchStatus.UPCOMING);
        return matchRepository.save(match);
    }

    private User saveUser(UserStatus status, String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + status.name().toLowerCase()
                + "." + System.nanoTime() + "@gotham.com");
        user.setPassword("encoded-password");
        user.setRole(Role.PLAYER);
        user.setStatus(status);
        return userRepository.save(user);
    }

    private void saveMatchSquad(Match match, User player) {
        MatchSquad squad = new MatchSquad();
        squad.setMatch(match);
        squad.setUser(player);
        squad.setIsPlayingXi(true);
        matchSquadRepository.save(squad);
    }

    private SaveScorecardRequest requestWithBatting(BattingEntryRequest entry) {
        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBattingEntries(List.of(entry));
        return request;
    }

    private SaveScorecardRequest requestWithBowling(BowlingEntryRequest entry) {
        SaveScorecardRequest request = minimalRequest();
        request.getInnings().getFirst().setBowlingEntries(List.of(entry));
        return request;
    }

    private BattingEntryRequest battingEntry(Long playerId, String externalName, int position) {
        BattingEntryRequest entry = new BattingEntryRequest();
        entry.setPlayerId(playerId);
        entry.setExternalPlayerName(externalName);
        entry.setBattingPosition(position);
        entry.setRuns(32);
        entry.setBallsFaced(24);
        entry.setFours(4);
        entry.setSixes(1);
        entry.setDismissed(true);
        entry.setDismissalText("c fielder b bowler");
        return entry;
    }

    private BowlingEntryRequest bowlingEntry(Long playerId, String externalName) {
        BowlingEntryRequest entry = new BowlingEntryRequest();
        entry.setPlayerId(playerId);
        entry.setExternalPlayerName(externalName);
        entry.setLegalBalls(18);
        entry.setMaidens(1);
        entry.setRunsConceded(20);
        entry.setWickets(2);
        entry.setWides(1);
        entry.setNoBalls(0);
        return entry;
    }

    private SaveScorecardRequest minimalRequest() {
        SaveInningsRequest innings = new SaveInningsRequest();
        innings.setInningsNumber(1);
        innings.setBattingTeamName("Gotham Knights");
        innings.setRuns(120);
        innings.setWickets(6);
        innings.setLegalBalls(118);
        innings.setTotalExtras(0);
        innings.setWides(0);
        innings.setNoBalls(0);
        innings.setByes(0);
        innings.setLegByes(0);
        innings.setPenaltyRuns(0);

        SaveScorecardRequest request = new SaveScorecardRequest();
        request.setOutcome(MatchOutcome.NO_RESULT);
        request.setInnings(List.of(innings));
        return request;
    }
}
