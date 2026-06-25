package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private Long id;
    private boolean pushEnabled;
    private boolean muteAllChats;
    private boolean muteGroupChats;
    private boolean muteMatchChats;
    private boolean muteEventChats;
}