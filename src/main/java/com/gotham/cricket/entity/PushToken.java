package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ removed unique = true — one user can have multiple devices
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    // expo token itself stays unique — same device never saved twice
    @Column(name = "expo_push_token", nullable = false, unique = true, length = 255)
    private String expoPushToken;
}