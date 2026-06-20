package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.SaveInningsRequest;
import com.gotham.cricket.dto.scorecard.SaveScorecardRequest;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.MatchScorecard;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.exception.ScorecardAlreadyExistsException;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.MatchScorecardRepository;
import com.gotham.cricket.repository.TeamRepository;
import org.junit.jupiter.api.Test;
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
    @Autowired private TeamRepository teamRepository;

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
