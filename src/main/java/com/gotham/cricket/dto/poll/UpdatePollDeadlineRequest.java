package com.gotham.cricket.dto.poll;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdatePollDeadlineRequest {

    /** New deadline. Pass null to remove the deadline entirely. Must be in the future when set. */
    private LocalDateTime deadlineAt;
}
