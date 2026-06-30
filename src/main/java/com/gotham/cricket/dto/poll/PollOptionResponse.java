package com.gotham.cricket.dto.poll;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PollOptionResponse {
    private Long optionId;
    private String optionText;
    private int displayOrder;
    /** Number of votes this option received. */
    private int voteCount;
    /** Percentage of total voters who selected this option (0–100). */
    private double percentage;
}
