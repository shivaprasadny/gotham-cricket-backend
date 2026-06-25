package com.gotham.cricket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceRequest {
    private boolean pushEnabled;
    private boolean muteAllChats;
    private boolean muteGroupChats;
    private boolean muteMatchChats;
    private boolean muteEventChats;
}