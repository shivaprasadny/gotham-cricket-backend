package com.gotham.cricket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationCodeRequest {
    private String email;
}