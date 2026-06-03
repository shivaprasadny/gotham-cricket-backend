package com.gotham.cricket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

//    // Runs every day at 10:00 AM server time
    @Scheduled(cron = "0 0 10 * * *", zone = "America/New_York")
//    @Scheduled(cron = "0 */1 * * * *")
    public void sendDailyReminders() {

        // Send availability reminders
        reminderService.sendAvailabilityReminders();

        // Send pending payment reminders
        reminderService.sendPendingPaymentReminders();
    }
}