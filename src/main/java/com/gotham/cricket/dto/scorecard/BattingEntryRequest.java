package com.gotham.cricket.dto.scorecard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BattingEntryRequest {

    private Long playerId;

    @Size(max = 255)
    private String externalPlayerName;

    @Min(1)
    private Integer battingPosition;

    @Min(0)
    private Integer runs;

    @Min(0)
    private Integer ballsFaced;

    @Min(0)
    private Integer fours;

    @Min(0)
    private Integer sixes;

    private Boolean dismissed = false;

    @Size(max = 1000)
    private String dismissalText;

    private Boolean didNotBat = false;
    private Boolean retiredHurt = false;
}
