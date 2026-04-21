package com.gotham.cricket.dto;

import com.gotham.cricket.enums.FeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request for custom split fee creation.
 */
@Data
public class CreateSplitFeeRequest {

    // Fee title
    @NotBlank(message = "Title is required")
    private String title;

    // Fee type
    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    // Due date
    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    // Optional description
    private String description;

    // Optional match link
    private Long matchId;

    // Optional event link
    private Long eventId;

    // Optional team link
    private Long teamId;

    // Optional season
    private String season;

    // Selected player splits
    private List<PlayerFeeSplitItem> splits;
}