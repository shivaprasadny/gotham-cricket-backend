package com.gotham.cricket.service;

import com.gotham.cricket.entity.FeeAssignment;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.FeeStatus;
import com.gotham.cricket.repository.AvailabilityRepository;
import com.gotham.cricket.repository.FeeAssignmentRepository;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final NotificationService notificationService;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final AvailabilityRepository availabilityRepository;

    // Daily reminder for users who did not mark availability
    public void sendAvailabilityReminders() {

        // Get today's date/time
        LocalDateTime now = LocalDateTime.now();

        // Start of current week = Monday 00:00
        LocalDateTime startOfWeek = now
                .with(java.time.DayOfWeek.MONDAY)
                .toLocalDate()
                .atStartOfDay();

        // End of current week = next Monday 00:00
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);

        // Get only matches happening this week
        List<Match> matchesThisWeek = matchRepository.findAll()
                .stream()
                .filter(match -> match.getMatchDate() != null)
                .filter(match -> !match.getMatchDate().isBefore(startOfWeek))
                .filter(match -> match.getMatchDate().isBefore(endOfWeek))
                .toList();

        // Get all users
        List<User> users = userRepository.findAll();

        // Loop through each user
        for (User user : users) {

            // Find if this user has at least one unmarked match this week
            boolean hasUnmarkedMatchThisWeek = false;

            for (Match match : matchesThisWeek) {

                // Check if user already marked availability for this match
                boolean alreadyMarked =
                        availabilityRepository.existsByUserAndMatch(user, match);

                // If not marked, user needs one reminder
                if (!alreadyMarked) {
                    hasUnmarkedMatchThisWeek = true;
                    break;
                }
            }

            // Send only ONE reminder per user
            if (hasUnmarkedMatchThisWeek) {
                notificationService.createForUserIds(
                        List.of(user.getId()),
                        "Availability Reminder",
                        "Please mark your availability for this week's match.",
                        "AVAILABILITY",
                        "Matches",
                        null
                );
            }
        }
    }

    // Daily reminder for users with unpaid or submitted-but-not-confirmed fees
    public void sendPendingPaymentReminders() {

        // Pending payment statuses
        List<FeeStatus> pendingStatuses = List.of(
                FeeStatus.UNPAID,
                FeeStatus.PAYMENT_SUBMITTED
        );

        // Get all pending fee assignments
        List<FeeAssignment> pendingAssignments =
                feeAssignmentRepository.findByStatusIn(pendingStatuses);

        log.info("Running payment reminders");
        log.info("Pending assignments count: {}", pendingAssignments.size());

        // Group pending fees by user ID instead of User object
        Map<Long, List<FeeAssignment>> pendingByUser =
                pendingAssignments.stream()
                        .filter(assignment -> assignment.getUser() != null)
                        .collect(Collectors.groupingBy(
                                assignment -> assignment.getUser().getId()
                        ));

        // Send only one payment reminder per user
        for (Map.Entry<Long, List<FeeAssignment>> entry : pendingByUser.entrySet()) {

            Long userId = entry.getKey();
            List<FeeAssignment> userFees = entry.getValue();

            // Calculate total pending amount for this user
            double totalPending = userFees.stream()
                    .mapToDouble(FeeAssignment::getAmount)
                    .sum();

            // Send one reminder to this user
            notificationService.createForUserIds(
                    List.of(userId),
                    "Payment Reminder",
                    "You have " + userFees.size()
                            + " pending payment(s), total $"
                            + totalPending,
                    "FEE",
                    "MyFees",
                    null
            );
        }
    }
}