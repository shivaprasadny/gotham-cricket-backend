package com.gotham.cricket.dto.poll;

import com.gotham.cricket.enums.PollAudienceType;
import com.gotham.cricket.enums.PollType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePollRequest {

    @NotBlank
    @Size(max = 500)
    private String question;

    @NotNull
    private PollType pollType;

    @NotNull
    private PollAudienceType audienceType;

    /** Required when audienceType = CUSTOM. IDs of approved users to include. */
    private List<Long> audienceUserIds;

    @Valid
    @NotNull
    @Size(min = 2, max = 10, message = "A poll needs between 2 and 10 options")
    private List<@NotBlank @Size(max = 500) String> options;

    /** Optional deadline; must be in the future when supplied. */
    private LocalDateTime deadlineAt;
}
