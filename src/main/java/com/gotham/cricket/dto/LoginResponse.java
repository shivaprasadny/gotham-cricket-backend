package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private String token;
    private String message;
    private String profileImageUrl;
}