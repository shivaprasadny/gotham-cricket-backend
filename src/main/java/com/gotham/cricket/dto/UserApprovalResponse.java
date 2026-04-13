package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserApprovalResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
}