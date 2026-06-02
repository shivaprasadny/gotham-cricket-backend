package com.gotham.cricket.controller;

import com.gotham.cricket.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailService emailService;
    @PostMapping("/email")
    public String sendTestEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");

        emailService.sendEmail(
                to,
                "Test Email",
                "Working fine"
        );

        return "Email sent";
    }
}