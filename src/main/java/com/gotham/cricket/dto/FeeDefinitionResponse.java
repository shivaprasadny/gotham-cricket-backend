package com.gotham.cricket.dto;

import com.gotham.cricket.enums.FeeAssignmentType;
import com.gotham.cricket.enums.FeeType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for master fee definition info.
 */
@Data
@AllArgsConstructor
public class FeeDefinitionResponse {

    private Long id;
    private String title;
    private FeeType feeType;
    private Double amount;
    private LocalDateTime dueDate;
    private String description;
    private Long matchId;
    private Long eventId;
    private Long teamId;
    private String season;
    private FeeAssignmentType assignmentType;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean active;
}