package com.gotham.cricket.dto.scorecard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FieldingEntryRequest {

    @NotNull
    private Long playerId;

    @Min(0)
    private Integer catches;

    @Min(0)
    private Integer droppedCatches;

    @Min(0)
    private Integer runOuts;

    @Min(0)
    private Integer stumpings;
}
