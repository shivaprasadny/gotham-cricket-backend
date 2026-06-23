package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.*;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.enums.TossDecision;
import com.gotham.cricket.enums.DismissalType;
import com.gotham.cricket.exception.ScorecardAlreadyExistsException;
import com.gotham.cricket.exception.ScorecardAlreadyPublishedException;
import com.gotham.cricket.exception.ScorecardNotFoundException;
import com.gotham.cricket.exception.ScorecardValidationException;
import com.gotham.cricket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScorecardService {

    private static final Logger log = LoggerFactory.getLogger(ScorecardService.class);

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MatchScorecardRepository matchScorecardRepository;
    private final InningsScoreRepository inningsScoreRepository;
    private final BattingPerformanceRepository battingPerformanceRepository;
    private final BowlingPerformanceRepository bowlingPerformanceRepository;
    private final FieldingPerformanceRepository fieldingPerformanceRepository;
    private final NotificationService notificationService;

    @Transactional
    public ScorecardResponse createDraft(Long matchId, SaveScorecardRequest request, String authenticatedEmail) {
        Match match = getMatch(matchId);

        if (matchScorecardRepository.existsByMatchId(matchId)) {
            throw new ScorecardAlreadyExistsException("A scorecard already exists for this match");
        }

        validateRequestBasics(request);

        MatchScorecard scorecard = new MatchScorecard();
        scorecard.setMatch(match);
        scorecard.setCreatedBy(authenticatedEmail);
        scorecard.setUpdatedBy(authenticatedEmail);
        applyMetaFields(scorecard, request);

        MatchScorecard savedScorecard = matchScorecardRepository.save(scorecard);
        saveChildren(savedScorecard, request);
        validatePersistedScorecard(savedScorecard);
        return buildResponse(savedScorecard, authenticatedEmail, true);
    }

    @Transactional
    public ScorecardResponse updateDraft(Long matchId, SaveScorecardRequest request, String authenticatedEmail) {
        MatchScorecard scorecard = getScorecard(matchId);
        if (scorecard.getStatus() != ScorecardStatus.DRAFT) {
            throw new ScorecardAlreadyPublishedException("Only draft scorecards can be updated");
        }

        validateRequestBasics(request);
        replaceChildren(scorecard.getId());
        applyMetaFields(scorecard, request);
        scorecard.setUpdatedBy(authenticatedEmail);

        MatchScorecard saved = matchScorecardRepository.save(scorecard);
        saveChildren(saved, request);
        validatePersistedScorecard(saved);
        return buildResponse(saved, authenticatedEmail, true);
    }

    @Transactional
    public ScorecardResponse getScorecard(Long matchId, Authentication authentication) {
        MatchScorecard scorecard = getScorecard(matchId);
        boolean adminOrCaptain = isAdminOrCaptain(authentication);
        if (scorecard.getStatus() == ScorecardStatus.DRAFT && !adminOrCaptain) {
            throw new ScorecardNotFoundException("Published scorecard not found for this match");
        }
        return buildResponse(scorecard, authentication.getName(), adminOrCaptain);
    }

    @Transactional
    public ScorecardResponse publishScorecard(Long matchId, String authenticatedEmail) {
        MatchScorecard scorecard = getScorecard(matchId);
        if (scorecard.getStatus() == ScorecardStatus.PUBLISHED) {
            throw new ScorecardAlreadyPublishedException("Scorecard is already published");
        }

        validatePersistedScorecard(scorecard);
        List<InningsScore> innings = inningsScoreRepository.findByScorecardIdOrderByInningsNumberAsc(scorecard.getId());
        if (innings.isEmpty()) {
            throw new ScorecardValidationException("At least one innings is required before publishing");
        }
        if ((scorecard.getOutcome() == MatchOutcome.WIN || scorecard.getOutcome() == MatchOutcome.LOSS || scorecard.getOutcome() == MatchOutcome.TIE)
                && innings.size() < 2) {
            throw new ScorecardValidationException("A completed result requires two innings");
        }

        scorecard.setStatus(ScorecardStatus.PUBLISHED);
        scorecard.setPublishedAt(java.time.LocalDateTime.now());
        scorecard.setUpdatedBy(authenticatedEmail);
        scorecard.setResultSummary(resolveResultSummary(scorecard));

        if (scorecard.getMatch() != null) {
            Match match = scorecard.getMatch();
            match.setStatus(MatchStatus.COMPLETED);
            matchRepository.save(match);
        }

        MatchScorecard saved = matchScorecardRepository.save(scorecard);

        try {
            notificationService.createForAllApprovedUsers(
                    "Scorecard Published",
                    saved.getMatch().getHomeTeam().getTeamName() + " scorecard is now available",
                    "SCORECARD",
                    "Scorecard",
                    saved.getMatch().getId()
            );
        } catch (Exception ex) {
            // Notification delivery must not roll back the scorecard publish.
            log.warn("Scorecard publish notification failed for match {}", matchId, ex);
        }

        return buildResponse(saved, authenticatedEmail, true);
    }

    @Transactional
    public ScorecardResponse reopenScorecard(Long matchId, String authenticatedEmail) {
        MatchScorecard scorecard = getScorecard(matchId);
        if (scorecard.getStatus() != ScorecardStatus.PUBLISHED) {
            throw new ScorecardValidationException("Only published scorecards can be reopened");
        }

        scorecard.setStatus(ScorecardStatus.DRAFT);
        scorecard.setPublishedAt(null);
        scorecard.setUpdatedBy(authenticatedEmail);
        MatchScorecard saved = matchScorecardRepository.save(scorecard);
        return buildResponse(saved, authenticatedEmail, true);
    }

    @Transactional
    public String deleteDraft(Long matchId, String authenticatedEmail) {
        MatchScorecard scorecard = getScorecard(matchId);
        if (scorecard.getStatus() != ScorecardStatus.DRAFT) {
            throw new ScorecardValidationException("Only draft scorecards can be deleted");
        }

        Long scorecardId = scorecard.getId();
        try {
            deleteChildrenByScorecardId(scorecardId);
            matchScorecardRepository.deleteById(scorecardId);
            matchScorecardRepository.flush();
            log.info("Deleted draft scorecard {} for match {} by {}", scorecardId, matchId, authenticatedEmail);
            return "Draft scorecard deleted successfully";
        } catch (RuntimeException exception) {
            log.error("Failed to delete draft scorecard {} for match {}", scorecardId, matchId, exception);
            throw new ScorecardValidationException(
                    "Could not delete the draft scorecard. Please try again."
            );
        }
    }

    @Transactional
    public void validateScorecard(MatchScorecard scorecard) {
        validatePersistedScorecard(scorecard);
    }

    private Match getMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ScorecardNotFoundException("Match not found with id: " + matchId));
    }

    private MatchScorecard getScorecard(Long matchId) {
        return matchScorecardRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ScorecardNotFoundException("Scorecard not found for match id: " + matchId));
    }

    private void applyMetaFields(MatchScorecard scorecard, SaveScorecardRequest request) {
        scorecard.setTossWinnerTeam(resolveTeam(request.getTossWinnerTeamId()));
        scorecard.setTossWinnerName(normalize(request.getTossWinnerName()));
        scorecard.setTossDecision(request.getTossDecision());
        scorecard.setOutcome(request.getOutcome());
        scorecard.setWinningTeam(resolveTeam(request.getWinningTeamId()));
        scorecard.setWinningTeamName(normalize(request.getWinningTeamName()));
        scorecard.setWinningMarginRuns(request.getWinningMarginRuns());
        scorecard.setWinningMarginWickets(request.getWinningMarginWickets());
        scorecard.setPlayerOfMatch(resolveApprovedUser(request.getPlayerOfMatchId()));
        scorecard.setResultSummary(normalize(request.getResultSummary()));
    }

    private void saveChildren(MatchScorecard scorecard, SaveScorecardRequest request) {
        for (SaveInningsRequest inningsRequest : request.getInnings()) {
            validateInningsRequest(inningsRequest);

            InningsScore innings = new InningsScore();
            innings.setScorecard(scorecard);
            innings.setInningsNumber(inningsRequest.getInningsNumber());
            innings.setBattingTeam(resolveTeam(inningsRequest.getBattingTeamId()));
            innings.setBattingTeamName(resolveTeamName(inningsRequest.getBattingTeamId(), inningsRequest.getBattingTeamName()));
            innings.setRuns(inningsRequest.getRuns());
            innings.setWickets(inningsRequest.getWickets());
            innings.setLegalBalls(inningsRequest.getLegalBalls());
            innings.setTotalExtras(inningsRequest.getTotalExtras());
            innings.setWides(inningsRequest.getWides());
            innings.setNoBalls(inningsRequest.getNoBalls());
            innings.setByes(inningsRequest.getByes());
            innings.setLegByes(inningsRequest.getLegByes());
            innings.setPenaltyRuns(inningsRequest.getPenaltyRuns());
            innings.setDeclared(Boolean.TRUE.equals(inningsRequest.getDeclared()));
            innings.setAllOut(Boolean.TRUE.equals(inningsRequest.getAllOut()));

            InningsScore savedInnings = inningsScoreRepository.save(innings);
            saveBattingRows(savedInnings, inningsRequest.getBattingEntries());
            saveBowlingRows(savedInnings, inningsRequest.getBowlingEntries());
            saveFieldingRows(savedInnings, inningsRequest.getFieldingEntries());
        }
    }

    private void saveBattingRows(InningsScore innings, List<BattingEntryRequest> battingEntries) {
        if (battingEntries == null) {
            return;
        }

        Set<Integer> positions = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();

        for (BattingEntryRequest entry : battingEntries) {
            validateBattingEntry(entry);
            if (!positions.add(entry.getBattingPosition())) {
                throw new ScorecardValidationException("Duplicate batting position in innings " + innings.getInningsNumber());
            }

            Long playerId = entry.getPlayerId();
            if (playerId != null && !playerIds.add(playerId)) {
                throw new ScorecardValidationException("Duplicate batting player in innings " + innings.getInningsNumber());
            }

            BattingPerformance row = new BattingPerformance();
            row.setInnings(innings);
            row.setPlayer(playerId != null ? resolveApprovedUser(playerId) : null);
            row.setExternalPlayerName(normalize(entry.getExternalPlayerName()));
            row.setBattingPosition(entry.getBattingPosition());
            row.setRuns(defaultZero(entry.getRuns()));
            row.setBallsFaced(defaultZero(entry.getBallsFaced()));
            row.setFours(defaultZero(entry.getFours()));
            row.setSixes(defaultZero(entry.getSixes()));
            DismissalType dismissalType = DismissalTypeResolver.resolve(entry);
            row.setDismissalType(dismissalType);
            row.setDismissed(DismissalTypeResolver.countsAsDismissal(dismissalType));
            row.setDismissalText(normalize(entry.getDismissalText()));
            row.setDidNotBat(dismissalType == DismissalType.DID_NOT_BAT);
            row.setRetiredHurt(dismissalType == DismissalType.RETIRED_HURT);
            battingPerformanceRepository.save(row);
        }
    }

    private void saveBowlingRows(InningsScore innings, List<BowlingEntryRequest> bowlingEntries) {
        if (bowlingEntries == null) {
            return;
        }

        Set<Long> playerIds = new HashSet<>();
        for (int index = 0; index < bowlingEntries.size(); index++) {
            BowlingEntryRequest entry = bowlingEntries.get(index);
            validateBowlingEntry(entry);
            Long playerId = entry.getPlayerId();
            if (playerId != null && !playerIds.add(playerId)) {
                throw new ScorecardValidationException("Duplicate bowling player in innings " + innings.getInningsNumber());
            }

            BowlingPerformance row = new BowlingPerformance();
            row.setInnings(innings);
            row.setPlayer(playerId != null ? resolveApprovedUser(playerId) : null);
            row.setExternalPlayerName(normalize(entry.getExternalPlayerName()));
            // Preserve the manager-selected bowling order for the scorecard display.
            row.setBowlingPosition(index + 1);
            row.setLegalBalls(defaultZero(entry.getLegalBalls()));
            row.setMaidens(defaultZero(entry.getMaidens()));
            row.setRunsConceded(defaultZero(entry.getRunsConceded()));
            row.setWickets(defaultZero(entry.getWickets()));
            row.setWides(defaultZero(entry.getWides()));
            row.setNoBalls(defaultZero(entry.getNoBalls()));
            row.setDotBalls(defaultZero(entry.getDotBalls()));
            bowlingPerformanceRepository.save(row);
        }
    }

    private void saveFieldingRows(InningsScore innings, List<FieldingEntryRequest> fieldingEntries) {
        if (fieldingEntries == null) {
            return;
        }

        Set<Long> playerIds = new HashSet<>();
        for (FieldingEntryRequest entry : fieldingEntries) {
            validateFieldingEntry(entry);
            if (!playerIds.add(entry.getPlayerId())) {
                throw new ScorecardValidationException("Duplicate fielding player in innings " + innings.getInningsNumber());
            }

            FieldingPerformance row = new FieldingPerformance();
            row.setInnings(innings);
            row.setPlayer(resolveApprovedUser(entry.getPlayerId()));
            row.setCatches(defaultZero(entry.getCatches()));
            row.setDroppedCatches(defaultZero(entry.getDroppedCatches()));
            row.setRunOuts(defaultZero(entry.getRunOuts()));
            row.setStumpings(defaultZero(entry.getStumpings()));
            fieldingPerformanceRepository.save(row);
        }
    }

    private void replaceChildren(Long scorecardId) {
        deleteChildrenByScorecardId(scorecardId);
    }

    private void deleteChildrenByScorecardId(Long scorecardId) {
        // Delete from the leaves upward. Using only IDs avoids retaining
        // managed innings entities after bulk JPQL deletion.
        List<Long> inningsIds = inningsScoreRepository.findIdsByScorecardId(scorecardId);
        if (!inningsIds.isEmpty()) {
            battingPerformanceRepository.deleteByInningsIds(inningsIds);
            bowlingPerformanceRepository.deleteByInningsIds(inningsIds);
            fieldingPerformanceRepository.deleteByInningsIds(inningsIds);
            battingPerformanceRepository.flush();
            bowlingPerformanceRepository.flush();
            fieldingPerformanceRepository.flush();
        }
        inningsScoreRepository.deleteByScorecardId(scorecardId);
        inningsScoreRepository.flush();
    }

    private void validateRequestBasics(SaveScorecardRequest request) {
        if (request.getOutcome() == null) {
            throw new ScorecardValidationException("Match outcome is required");
        }
        if (request.getInnings() == null || request.getInnings().isEmpty()) {
            throw new ScorecardValidationException("At least one innings is required");
        }
        if (request.getInnings().size() > 2) {
            throw new ScorecardValidationException("A scorecard can contain at most two innings");
        }
        Set<Integer> inningsNumbers = new HashSet<>();
        for (SaveInningsRequest innings : request.getInnings()) {
            if (!inningsNumbers.add(innings.getInningsNumber())) {
                throw new ScorecardValidationException("Duplicate innings number: " + innings.getInningsNumber());
            }
        }
        if (inningsNumbers.contains(2) && !inningsNumbers.contains(1)) {
            throw new ScorecardValidationException("Innings 2 cannot exist without innings 1");
        }
    }

    private void validateInningsRequest(SaveInningsRequest innings) {
        normalizeLegacyExtras(innings);
        if (innings.getInningsNumber() == null || (innings.getInningsNumber() != 1 && innings.getInningsNumber() != 2)) {
            throw new ScorecardValidationException("Innings number must be 1 or 2");
        }
        if (innings.getRuns() == null || innings.getWickets() == null || innings.getLegalBalls() == null
                || innings.getTotalExtras() == null || innings.getWides() == null || innings.getNoBalls() == null
                || innings.getByes() == null || innings.getLegByes() == null || innings.getPenaltyRuns() == null) {
            throw new ScorecardValidationException("All innings totals are required");
        }
        if (innings.getWickets() < 0 || innings.getWickets() > 10) {
            throw new ScorecardValidationException("Wickets must be between 0 and 10");
        }
        if (innings.getWides() + innings.getNoBalls() + innings.getByes() + innings.getLegByes() + innings.getPenaltyRuns() != innings.getTotalExtras()) {
            throw new ScorecardValidationException("Extras components must equal total extras");
        }
        if (innings.getBattingEntries() != null && innings.getBattingEntries().size() > 11) {
            throw new ScorecardValidationException("An innings can contain at most 11 batting entries");
        }
        if (innings.getBowlingEntries() != null && innings.getBowlingEntries().size() > 11) {
            throw new ScorecardValidationException("An innings can contain at most 11 bowling entries");
        }
        if (innings.getFieldingEntries() != null && innings.getFieldingEntries().size() > 12) {
            throw new ScorecardValidationException("An innings can contain at most 12 fielding entries");
        }
        if (innings.getBattingEntries() != null) {
            long notOutCount = innings.getBattingEntries().stream()
                    .filter(entry -> DismissalTypeResolver.resolve(entry) == DismissalType.NOT_OUT)
                    .count();
            if (notOutCount > 2) {
                throw new ScorecardValidationException("An innings can have at most 2 Not Out batters");
            }
        }
        if (innings.getBattingEntries() != null) {
            for (BattingEntryRequest batting : innings.getBattingEntries()) {
                validateBattingEntry(batting);
            }
        }
        if (innings.getBowlingEntries() != null) {
            for (BowlingEntryRequest bowling : innings.getBowlingEntries()) {
                validateBowlingEntry(bowling);
            }
        }
        if (innings.getFieldingEntries() != null) {
            for (FieldingEntryRequest fielding : innings.getFieldingEntries()) {
                validateFieldingEntry(fielding);
            }
        }
    }

    /**
     * Older clients sent only totalExtras and treated the amount as wides.
     * Preserve those saved/request payloads while new clients send all fields.
     */
    private void normalizeLegacyExtras(SaveInningsRequest innings) {
        int total = defaultZero(innings.getTotalExtras());
        int wides = defaultZero(innings.getWides());
        int noBalls = defaultZero(innings.getNoBalls());
        int byes = defaultZero(innings.getByes());
        int legByes = defaultZero(innings.getLegByes());
        int penaltyRuns = defaultZero(innings.getPenaltyRuns());
        if (total > 0 && wides + noBalls + byes + legByes + penaltyRuns == 0) {
            wides = total;
        }
        innings.setTotalExtras(total);
        innings.setWides(wides);
        innings.setNoBalls(noBalls);
        innings.setByes(byes);
        innings.setLegByes(legByes);
        innings.setPenaltyRuns(penaltyRuns);
    }

    private void validateBattingEntry(BattingEntryRequest entry) {
        boolean hasPlayer = entry.getPlayerId() != null;
        boolean hasExternal = !isBlank(entry.getExternalPlayerName());
        if (hasPlayer == hasExternal) {
            throw new ScorecardValidationException("Each batting entry must have exactly one of internal player or external player name");
        }
        if (entry.getBattingPosition() == null || entry.getBattingPosition() <= 0) {
            throw new ScorecardValidationException("Batting position must be positive");
        }
        int runs = defaultZero(entry.getRuns());
        int balls = defaultZero(entry.getBallsFaced());
        int fours = defaultZero(entry.getFours());
        int sixes = defaultZero(entry.getSixes());
        DismissalType dismissalType = DismissalTypeResolver.resolve(entry);
        if (runs < 0 || balls < 0 || fours < 0 || sixes < 0) {
            throw new ScorecardValidationException("Batting values cannot be negative");
        }
        if (fours * 4 + sixes * 6 > runs) {
            throw new ScorecardValidationException("Boundary runs cannot exceed batter runs");
        }
        if (dismissalType == DismissalType.DID_NOT_BAT && (runs != 0 || balls != 0)) {
            throw new ScorecardValidationException("Did-not-bat entries must have zero runs and zero balls");
        }
        if (entry.getDismissalType() == DismissalType.OTHER && isBlank(entry.getDismissalText())) {
            throw new ScorecardValidationException("OTHER dismissal type requires dismissal text");
        }
    }

    private void validateBowlingEntry(BowlingEntryRequest entry) {
        boolean hasPlayer = entry.getPlayerId() != null;
        boolean hasExternal = !isBlank(entry.getExternalPlayerName());
        if (hasPlayer == hasExternal) {
            throw new ScorecardValidationException("Each bowling entry must have exactly one of internal player or external player name");
        }
        if (defaultZero(entry.getLegalBalls()) < 0 || defaultZero(entry.getMaidens()) < 0
                || defaultZero(entry.getRunsConceded()) < 0 || defaultZero(entry.getWickets()) < 0
                || defaultZero(entry.getWides()) < 0 || defaultZero(entry.getNoBalls()) < 0) {
            throw new ScorecardValidationException("Bowling values cannot be negative");
        }
        if (defaultZero(entry.getDotBalls()) < 0
                || defaultZero(entry.getDotBalls()) > defaultZero(entry.getLegalBalls())) {
            throw new ScorecardValidationException("Dot balls must be between 0 and legal balls");
        }
        if (defaultZero(entry.getWickets()) > 10) {
            throw new ScorecardValidationException("Wickets cannot exceed 10");
        }
        if (entry.getLegalBalls() != null) {
            int completedOvers = entry.getLegalBalls() / 6;
            if (entry.getMaidens() != null && entry.getMaidens() > completedOvers) {
                throw new ScorecardValidationException("Maidens cannot exceed completed overs");
            }
        }
    }

    private void validateFieldingEntry(FieldingEntryRequest entry) {
        if (entry.getPlayerId() == null) {
            throw new ScorecardValidationException("Fielding player is required");
        }
        if (defaultZero(entry.getCatches()) < 0 || defaultZero(entry.getDroppedCatches()) < 0
                || defaultZero(entry.getRunOuts()) < 0 || defaultZero(entry.getStumpings()) < 0) {
            throw new ScorecardValidationException("Fielding values cannot be negative");
        }
    }

    private void validatePersistedScorecard(MatchScorecard scorecard) {
        List<InningsScore> innings = inningsScoreRepository.findByScorecardIdOrderByInningsNumberAsc(scorecard.getId());
        if (innings.isEmpty()) {
            throw new ScorecardValidationException("A scorecard must contain at least one innings");
        }
        if (innings.size() > 2) {
            throw new ScorecardValidationException("A scorecard can contain at most two innings");
        }
        if (innings.size() == 2 && innings.get(0).getInningsNumber() != 1) {
            throw new ScorecardValidationException("Innings 2 cannot exist without innings 1");
        }

        for (InningsScore inningsScore : innings) {
            if (inningsScore.getRuns() < 0 || inningsScore.getWickets() < 0 || inningsScore.getLegalBalls() < 0 || inningsScore.getTotalExtras() < 0) {
                throw new ScorecardValidationException("Innings totals cannot be negative");
            }
            if (inningsScore.getWickets() > 10) {
                throw new ScorecardValidationException("Wickets cannot exceed 10");
            }
            if (inningsScore.getWides() + inningsScore.getNoBalls() + inningsScore.getByes() + inningsScore.getLegByes() + inningsScore.getPenaltyRuns()
                    != inningsScore.getTotalExtras()) {
                throw new ScorecardValidationException("Extras components must equal total extras");
            }

            List<BattingPerformance> battingRows = battingPerformanceRepository.findByInningsIdOrderByBattingPositionAsc(inningsScore.getId());
            List<BowlingPerformance> bowlingRows = bowlingPerformanceRepository.findByInningsId(inningsScore.getId());
            List<FieldingPerformance> fieldingRows = fieldingPerformanceRepository.findByInningsId(inningsScore.getId());

            if (battingRows.stream().map(BattingPerformance::getBattingPosition).collect(Collectors.toSet()).size() != battingRows.size()) {
                throw new ScorecardValidationException("Duplicate batting positions found");
            }
            if (battingRows.stream().map(row -> row.getPlayer() != null ? row.getPlayer().getId() : row.getExternalPlayerName()).collect(Collectors.toSet()).size() != battingRows.size()) {
                throw new ScorecardValidationException("Duplicate batting players found");
            }
            if (bowlingRows.stream().map(row -> row.getPlayer() != null ? row.getPlayer().getId() : row.getExternalPlayerName()).collect(Collectors.toSet()).size() != bowlingRows.size()) {
                throw new ScorecardValidationException("Duplicate bowling players found");
            }
            if (fieldingRows.stream().map(row -> row.getPlayer().getId()).collect(Collectors.toSet()).size() != fieldingRows.size()) {
                throw new ScorecardValidationException("Duplicate fielding players found");
            }

            int battingRuns = battingRows.stream().mapToInt(row -> defaultZero(row.getRuns())).sum();
            int bowlingWickets = bowlingRows.stream().mapToInt(row -> defaultZero(row.getWickets())).sum();
            if (battingRuns > inningsScore.getRuns()) {
                throw new ScorecardValidationException("Sum of batting runs cannot exceed innings runs");
            }
            if (bowlingWickets > inningsScore.getWickets()) {
                throw new ScorecardValidationException("Sum of bowler wickets cannot exceed innings wickets");
            }
        }

        validateMatchOutcome(scorecard, innings);
    }

    private void validateMatchOutcome(MatchScorecard scorecard, List<InningsScore> innings) {
        if (scorecard.getOutcome() == MatchOutcome.TIE) {
            if (innings.size() >= 2 && !Objects.equals(innings.get(0).getRuns(), innings.get(1).getRuns())) {
                throw new ScorecardValidationException("Tie scorecards must have equal innings scores");
            }
        }
        if (scorecard.getOutcome() == MatchOutcome.WIN || scorecard.getOutcome() == MatchOutcome.LOSS) {
            boolean hasMarginRuns = scorecard.getWinningMarginRuns() != null;
            boolean hasMarginWickets = scorecard.getWinningMarginWickets() != null;
            if (hasMarginRuns == hasMarginWickets) {
                throw new ScorecardValidationException("Winning margin must be expressed as either runs or wickets");
            }
        }
        if (scorecard.getOutcome() == MatchOutcome.NO_RESULT || scorecard.getOutcome() == MatchOutcome.ABANDONED) {
            if (scorecard.getWinningMarginRuns() != null || scorecard.getWinningMarginWickets() != null) {
                throw new ScorecardValidationException("No-result or abandoned matches should not have a winning margin");
            }
        }
    }

    private ScorecardResponse buildResponse(MatchScorecard scorecard, String viewerEmail, boolean includeDraft) {
        List<InningsScore> innings = inningsScoreRepository.findByScorecardIdOrderByInningsNumberAsc(scorecard.getId());
        if (scorecard.getStatus() == ScorecardStatus.DRAFT && !includeDraft) {
            throw new ScorecardNotFoundException("Published scorecard not found for this match");
        }

        List<InningsResponse> inningsResponses = innings.stream().map(this::buildInningsResponse).toList();

        Integer target = innings.isEmpty() ? null : innings.get(0).getRuns() + 1;
        Integer firstInningsTotal = innings.isEmpty() ? null : innings.get(0).getRuns();
        Integer chaseTotal = innings.size() > 1 ? innings.get(1).getRuns() : null;
        String topScorer = findTopScorer(innings);
        String bestBowler = findBestBowler(innings);

        return new ScorecardResponse(
                scorecard.getId(),
                scorecard.getMatch().getId(),
                buildMatchSummary(scorecard.getMatch()),
                scorecard.getTossWinnerTeam() != null ? scorecard.getTossWinnerTeam().getId() : null,
                scorecard.getTossWinnerName(),
                scorecard.getTossDecision(),
                scorecard.getOutcome(),
                scorecard.getWinningTeam() != null ? scorecard.getWinningTeam().getId() : null,
                scorecard.getWinningTeamName(),
                scorecard.getWinningMarginRuns(),
                scorecard.getWinningMarginWickets(),
                resolveResultSummary(scorecard),
                firstInningsTotal,
                chaseTotal,
                topScorer,
                bestBowler,
                scorecard.getPlayerOfMatch() != null ? scorecard.getPlayerOfMatch().getId() : null,
                scorecard.getPlayerOfMatch() != null ? scorecard.getPlayerOfMatch().getFullName() : null,
                target,
                scorecard.getStatus(),
                scorecard.getPublishedAt(),
                inningsResponses
        );
    }

    private InningsResponse buildInningsResponse(InningsScore inningsScore) {
        List<BattingPerformanceResponse> batting = battingPerformanceRepository.findByInningsIdOrderByBattingPositionAsc(inningsScore.getId())
                .stream()
                .map(this::buildBattingResponse)
                .toList();
        List<BowlingPerformanceResponse> bowling = bowlingPerformanceRepository
                .findByInningsIdOrderByBowlingPositionAscIdAsc(inningsScore.getId())
                .stream()
                .map(this::buildBowlingResponse)
                .toList();
        List<FieldingPerformanceResponse> fielding = fieldingPerformanceRepository.findByInningsId(inningsScore.getId())
                .stream()
                .map(this::buildFieldingResponse)
                .toList();

        return new InningsResponse(
                inningsScore.getId(),
                inningsScore.getInningsNumber(),
                inningsScore.getBattingTeam() != null ? inningsScore.getBattingTeam().getId() : null,
                inningsScore.getBattingTeamName(),
                inningsScore.getRuns(),
                inningsScore.getWickets(),
                inningsScore.getLegalBalls(),
                ScorecardMath.formatOvers(inningsScore.getLegalBalls()),
                defaultZero(inningsScore.getTotalExtras()),
                defaultZero(inningsScore.getWides()),
                defaultZero(inningsScore.getNoBalls()),
                defaultZero(inningsScore.getByes()),
                defaultZero(inningsScore.getLegByes()),
                defaultZero(inningsScore.getPenaltyRuns()),
                inningsScore.isDeclared(),
                inningsScore.isAllOut(),
                batting,
                bowling,
                fielding
        );
    }

    private BattingPerformanceResponse buildBattingResponse(BattingPerformance row) {
        String name = row.getPlayer() != null ? row.getPlayer().getFullName() : row.getExternalPlayerName();
        DismissalType dismissalType = DismissalTypeResolver.resolve(row);
        String dismissal = dismissalType == DismissalType.DID_NOT_BAT
                ? "Did not bat"
                : dismissalType == DismissalType.RETIRED_HURT ? "Retired hurt" : row.getDismissalText();
        return new BattingPerformanceResponse(
                row.getPlayer() != null ? row.getPlayer().getId() : null,
                name,
                defaultZero(row.getRuns()),
                defaultZero(row.getBallsFaced()),
                defaultZero(row.getFours()),
                defaultZero(row.getSixes()),
                dismissalType,
                dismissal,
                ScorecardMath.strikeRate(defaultZero(row.getRuns()), defaultZero(row.getBallsFaced()))
        );
    }

    private BowlingPerformanceResponse buildBowlingResponse(BowlingPerformance row) {
        String name = row.getPlayer() != null ? row.getPlayer().getFullName() : row.getExternalPlayerName();
        return new BowlingPerformanceResponse(
                row.getPlayer() != null ? row.getPlayer().getId() : null,
                name,
                ScorecardMath.formatOvers(defaultZero(row.getLegalBalls())),
                defaultZero(row.getMaidens()),
                defaultZero(row.getRunsConceded()),
                defaultZero(row.getWickets()),
                ScorecardMath.economy(defaultZero(row.getRunsConceded()), defaultZero(row.getLegalBalls())),
                defaultZero(row.getWides()),
                defaultZero(row.getNoBalls()),
                defaultZero(row.getDotBalls()),
                defaultZero(row.getWides()) + defaultZero(row.getNoBalls())
        );
    }

    private FieldingPerformanceResponse buildFieldingResponse(FieldingPerformance row) {
        int catches = defaultZero(row.getCatches());
        int droppedCatches = defaultZero(row.getDroppedCatches());
        int runOuts = defaultZero(row.getRunOuts());
        int stumpings = defaultZero(row.getStumpings());
        int catchChances = catches + droppedCatches;
        return new FieldingPerformanceResponse(
                row.getPlayer().getId(),
                row.getPlayer().getFullName(),
                catches,
                droppedCatches,
                runOuts,
                stumpings,
                catches + runOuts + stumpings,
                catchChances,
                catchChances == 0 ? 0d : ScorecardMath.round2(catches * 100.0 / catchChances)
        );
    }

    private String buildMatchSummary(Match match) {
        String away = match.getAwayTeam() != null ? match.getAwayTeam().getTeamName() : match.getExternalOpponentName();
        return match.getHomeTeam().getTeamName() + " vs " + away;
    }

    private String findTopScorer(List<InningsScore> innings) {
        return innings.stream()
                .flatMap(inning -> battingPerformanceRepository.findByInningsIdOrderByBattingPositionAsc(inning.getId()).stream())
                .max(Comparator.comparingInt((BattingPerformance b) -> defaultZero(b.getRuns())))
                .map(b -> b.getPlayer() != null ? b.getPlayer().getFullName() : b.getExternalPlayerName() + " (" + b.getRuns() + ")")
                .orElse(null);
    }

    private String findBestBowler(List<InningsScore> innings) {
        return innings.stream()
                .flatMap(inning -> bowlingPerformanceRepository.findByInningsId(inning.getId()).stream())
                .max(Comparator.comparingInt((BowlingPerformance b) -> defaultZero(b.getWickets()))
                        .thenComparingInt(b -> -defaultZero(b.getRunsConceded())))
                .map(b -> {
                    String name = b.getPlayer() != null ? b.getPlayer().getFullName() : b.getExternalPlayerName();
                    return name + " " + defaultZero(b.getWickets()) + "/" + defaultZero(b.getRunsConceded());
                })
                .orElse(null);
    }

    private String resolveResultSummary(MatchScorecard scorecard) {
        if (scorecard.getResultSummary() != null && !scorecard.getResultSummary().isBlank()) {
            return scorecard.getResultSummary();
        }
        if (scorecard.getOutcome() == MatchOutcome.TIE) {
            return "Match tied";
        }
        if (scorecard.getOutcome() == MatchOutcome.NO_RESULT) {
            return "No result";
        }
        if (scorecard.getOutcome() == MatchOutcome.ABANDONED) {
            return "Match abandoned";
        }

        String winner = scorecard.getWinningTeam() != null ? scorecard.getWinningTeam().getTeamName() : scorecard.getWinningTeamName();
        if (scorecard.getWinningMarginRuns() != null) {
            return winner + " won by " + scorecard.getWinningMarginRuns() + " runs";
        }
        if (scorecard.getWinningMarginWickets() != null) {
            return winner + " won by " + scorecard.getWinningMarginWickets() + " wickets";
        }
        return winner != null ? winner + " won" : null;
    }

    private Team resolveTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ScorecardValidationException("Team not found with id: " + teamId));
    }

    private String resolveTeamName(Long teamId, String fallbackName) {
        if (teamId != null) {
            Team team = resolveTeam(teamId);
            return team.getTeamName();
        }
        if (isBlank(fallbackName)) {
            throw new ScorecardValidationException("Batting team name is required when no internal team is selected");
        }
        return fallbackName.trim();
    }

    private User resolveApprovedUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ScorecardValidationException("User not found with id: " + userId));
        if (user.getStatus() == null || !"APPROVED".equalsIgnoreCase(user.getStatus().name())) {
            throw new ScorecardValidationException("User must be approved");
        }
        return user;
    }

    private boolean isAdminOrCaptain(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_CAPTAIN".equals(role));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
