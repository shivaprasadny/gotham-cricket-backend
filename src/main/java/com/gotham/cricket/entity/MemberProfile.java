package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "nickname")
    private String nickname;

    // Country code stored separately (e.g. "+1") so the frontend can
    // construct dial URIs and WhatsApp links independently of the number.
    @Column(name = "country_code", length = 10)
    private String countryCode;

    // Phone number digits only (no country code prefix).
    @Column(name = "phone")
    private String phone;

    @Column(name = "batting_style")
    private String battingStyle;

    @Column(name = "bowling_style")
    private String bowlingStyle;

    @Column(name = "player_type")
    private String playerType;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column
    private String gender;

    @Column
    private String dateOfBirth;

    // Contact privacy — nullable so we can distinguish "never set" (null = show by default)
    // from "explicitly hidden" (false).  Services treat null as true.
    @Column(name = "show_email")
    private Boolean showEmail;

    @Column(name = "show_phone")
    private Boolean showPhone;

    @Column(name = "show_whatsapp")
    private Boolean showWhatsApp;
}
