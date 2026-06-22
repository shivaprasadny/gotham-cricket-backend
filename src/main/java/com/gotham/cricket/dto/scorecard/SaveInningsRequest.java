package com.gotham.cricket.dto.scorecard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SaveInningsRequest {

    @NotNull
    @Min(1)
    private Integer inningsNumber;

    private Long battingTeamId;
    private String battingTeamName;

    @NotNull
    @Min(0)
    private Integer runs;

    @NotNull
    @Min(0)
    private Integer wickets;

    @NotNull
    @Min(0)
    private Integer legalBalls;

    @NotNull
    @Min(0)
    private Integer totalExtras;

    @NotNull
    @Min(0)
    private Integer wides;

    @NotNull
    @Min(0)
    private Integer noBalls;

    @NotNull
    @Min(0)
    private Integer byes;

    @NotNull
    @Min(0)
    private Integer legByes;

    @NotNull
    @Min(0)
    private Integer penaltyRuns;

    private Boolean declared = false;
    private Boolean allOut = false;

    @Valid
    @Size(max = 11)
    private List<BattingEntryRequest> battingEntries;

    @Valid
    @Size(max = 11)
    private List<BowlingEntryRequest> bowlingEntries;

    @Valid
    @Size(max = 12)
    private List<FieldingEntryRequest> fieldingEntries;
}
