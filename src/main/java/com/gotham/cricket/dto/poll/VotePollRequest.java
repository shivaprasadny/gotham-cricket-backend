package com.gotham.cricket.dto.poll;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VotePollRequest {

    /** One optionId for SINGLE_CHOICE, one or more for MULTIPLE_CHOICE. */
    @NotEmpty
    private List<Long> optionIds;
}
