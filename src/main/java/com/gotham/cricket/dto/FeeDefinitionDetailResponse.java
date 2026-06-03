package com.gotham.cricket.dto;

import com.gotham.cricket.enums.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDefinitionDetailResponse {
    private Long id;
    private String title;
    private FeeType feeType;
    private Double amount;
    private LocalDateTime dueDate;
    private String description;
    private List<FeeAssignmentResponse> assignments;
}