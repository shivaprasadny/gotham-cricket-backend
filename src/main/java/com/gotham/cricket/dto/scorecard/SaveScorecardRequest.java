package com.gotham.cricket.dto.scorecard;

import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.TossDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SaveScorecardRequest {

    private Long tossWinnerTeamId;
    private String tossWinnerName;

    private TossDecision tossDecision;
    private MatchOutcome outcome;

    private Long winningTeamId;
    private String winningTeamName;
    private Integer winningMarginRuns;
    private Integer winningMarginWickets;

    @Size(max = 2000)
    private String resultSummary;

    private Long playerOfMatchId;

    @Valid
    @Size(min = 1, max = 2)
    private List<SaveInningsRequest> innings;
}
