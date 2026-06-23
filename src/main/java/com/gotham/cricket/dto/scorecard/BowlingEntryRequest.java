package com.gotham.cricket.dto.scorecard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BowlingEntryRequest {

    private Long playerId;

    @Size(max = 255)
    private String externalPlayerName;

    @Min(0)
    private Integer legalBalls;

    @Min(0)
    private Integer maidens;

    @Min(0)
    private Integer runsConceded;

    @Min(0)
    private Integer wickets;

    @Min(0)
    private Integer wides;

    @Min(0)
    private Integer noBalls;

    @Min(0)
    private Integer dotBalls;
}
