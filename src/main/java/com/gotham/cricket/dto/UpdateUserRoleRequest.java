package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    private Role role;
}