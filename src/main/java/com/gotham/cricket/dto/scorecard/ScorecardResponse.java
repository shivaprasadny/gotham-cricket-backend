package com.gotham.cricket.dto.scorecard;

import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.enums.TossDecision;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ScorecardResponse {
    private Long scorecardId;
    private Long matchId;
    private String matchSummary;
    private Long tossWinnerTeamId;
    private String tossWinnerName;
    private TossDecision tossDecision;
    private MatchOutcome outcome;
    private Long winningTeamId;
    private String winningTeamName;
    private Integer winningMarginRuns;
    private Integer winningMarginWickets;
    private String resultSummary;
    private Integer firstInningsTotal;
    private Integer chaseTotal;
    private String topScorer;
    private String bestBowler;
    private Long playerOfMatchId;
    private String playerOfMatchName;
    private Integer target;
    private ScorecardStatus status;
    private LocalDateTime publishedAt;
    private List<InningsResponse> innings;
}
