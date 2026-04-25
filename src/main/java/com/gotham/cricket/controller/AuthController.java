package com.gotham.cricket.controller;

import com.gotham.cricket.dto.LoginRequest;
import com.gotham.cricket.dto.LoginResponse;
import com.gotham.cricket.dto.RegisterRequest;
import com.gotham.cricket.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

//    @PostMapping("/login")
//    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
//        return authService.login(request);
//    }


    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        System.out.println("🔥 LOGIN API HIT FROM MOBILE");
        System.out.println("Email: " + request.getEmail());

        return authService.login(request);
    }


}