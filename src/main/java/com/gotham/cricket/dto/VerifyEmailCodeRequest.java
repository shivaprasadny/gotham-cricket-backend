package com.gotham.cricket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailCodeRequest {
    private String email;
    private String code;
}