package com.gotham.cricket.service;

import com.gotham.cricket.dto.CreateFeeRequest;
import com.gotham.cricket.dto.FeeAssignmentResponse;
import com.gotham.cricket.dto.FeeDefinitionResponse;
import com.gotham.cricket.dto.FeeSummaryResponse;
import com.gotham.cricket.entity.FeeAssignment;
import com.gotham.cricket.entity.FeeDefinition;
import com.gotham.cricket.entity.TeamMember;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.FeeAssignmentType;
import com.gotham.cricket.enums.FeeStatus;
import com.gotham.cricket.repository.FeeAssignmentRepository;
import com.gotham.cricket.repository.FeeDefinitionRepository;
import com.gotham.cricket.repository.TeamMemberRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for fee management.
 *
 * Responsibilities:
 * - create master fee definition
 * - generate fee assignments based on assignment type
 * - fetch fee list/details
 * - fetch user-specific fee data
 */
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeDefinitionRepository feeDefinitionRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Creates a fee definition and automatically assigns it to users
     * based on the chosen assignment type.
     *
     * Supported in this first version:
     * - ALL_MEMBERS
     * - SELECTED_USERS
     * - TEAM_MEMBERS
     *
     * We'll add squad/event-based assignment later.
     */
    @Transactional
    public String createFee(String email, CreateFeeRequest request) {
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build and save the master fee definition first
        FeeDefinition feeDefinition = new FeeDefinition();
        feeDefinition.setTitle(request.getTitle());
        feeDefinition.setFeeType(request.getFeeType());
        feeDefinition.setAmount(request.getAmount());
        feeDefinition.setDueDate(request.getDueDate());
        feeDefinition.setDescription(request.getDescription());
        feeDefinition.setMatchId(request.getMatchId());
        feeDefinition.setEventId(request.getEventId());
        feeDefinition.setTeamId(request.getTeamId());
        feeDefinition.setSeason(request.getSeason());
        feeDefinition.setAssignmentType(request.getAssignmentType());
        feeDefinition.setCreatedBy(creator.getFullName());
        feeDefinition.setActive(true);

        feeDefinitionRepository.save(feeDefinition);

        // Resolve users who should receive this fee
        List<User> targetUsers = resolveTargetUsers(request);

        // Create one fee assignment per selected user
        List<FeeAssignment> assignments = new ArrayList<>();

        for (User user : targetUsers) {
            FeeAssignment assignment = new FeeAssignment();
            assignment.setFeeDefinition(feeDefinition);
            assignment.setUser(user);
            assignment.setAmount(request.getAmount());
            assignment.setDueDate(request.getDueDate());
            assignment.setStatus(FeeStatus.UNPAID);

            assignments.add(assignment);
        }

        feeAssignmentRepository.saveAll(assignments);

        return "Fee created and assigned successfully";
    }

    /**
     * Returns all fee definitions for admin/captain list screen.
     */
    public List<FeeDefinitionResponse> getAllFeeDefinitions() {
        return feeDefinitionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapFeeDefinitionToResponse)
                .toList();
    }

    /**
     * Returns one fee definition by id.
     */
    public FeeDefinitionResponse getFeeDefinitionById(Long feeDefinitionId) {
        FeeDefinition feeDefinition = feeDefinitionRepository.findById(feeDefinitionId)
                .orElseThrow(() -> new RuntimeException("Fee definition not found"));

        return mapFeeDefinitionToResponse(feeDefinition);
    }

    /**
     * Returns all user assignments under one fee definition.
     * Useful for admin/captain fee details screen.
     */
    public List<FeeAssignmentResponse> getAssignmentsByFeeDefinition(Long feeDefinitionId) {
        FeeDefinition feeDefinition = feeDefinitionRepository.findById(feeDefinitionId)
                .orElseThrow(() -> new RuntimeException("Fee definition not found"));

        return feeAssignmentRepository.findByFeeDefinitionOrderByDueDateAsc(feeDefinition)
                .stream()
                .map(this::mapFeeAssignmentToResponse)
                .toList();
    }

    /**
     * Returns all fee assignments for the currently logged-in user.
     * Useful for My Fees screen.
     */
    public List<FeeAssignmentResponse> getMyFees(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return feeAssignmentRepository.findByUserOrderByDueDateAsc(user)
                .stream()
                .map(this::mapFeeAssignmentToResponse)
                .toList();
    }

    /**
     * Returns summary stats for the logged-in user.
     * Useful for:
     * - home screen alert card
     * - My Fees top summary
     */
    public FeeSummaryResponse getMyFeeSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FeeAssignment> assignments = feeAssignmentRepository.findByUserOrderByDueDateAsc(user);

        double totalOutstandingAmount = assignments.stream()
                .filter(a -> a.getStatus() == FeeStatus.UNPAID || a.getStatus() == FeeStatus.PAYMENT_SUBMITTED)
                .mapToDouble(FeeAssignment::getAmount)
                .sum();

        long unpaidCount = assignments.stream()
                .filter(a -> a.getStatus() == FeeStatus.UNPAID)
                .count();

        long overdueCount = assignments.stream()
                .filter(a -> a.getStatus() == FeeStatus.UNPAID)
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(LocalDateTime.now()))
                .count();

        long paymentSubmittedCount = assignments.stream()
                .filter(a -> a.getStatus() == FeeStatus.PAYMENT_SUBMITTED)
                .count();

        long paidCount = assignments.stream()
                .filter(a -> a.getStatus() == FeeStatus.PAID)
                .count();

        return new FeeSummaryResponse(
                totalOutstandingAmount,
                unpaidCount,
                overdueCount,
                paymentSubmittedCount,
                paidCount
        );
    }

    /**
     * Resolves which users should receive the fee based on assignment type.
     *
     * First version supports:
     * - ALL_MEMBERS
     * - SELECTED_USERS
     * - TEAM_MEMBERS
     */
    private List<User> resolveTargetUsers(CreateFeeRequest request) {
        FeeAssignmentType assignmentType = request.getAssignmentType();

        switch (assignmentType) {
            case ALL_MEMBERS -> {
                // We assign only approved users
                return userRepository.findAll()
                        .stream()
                        .filter(user -> user.getStatus() != null && user.getStatus().name().equals("APPROVED"))
                        .toList();
            }

            case SELECTED_USERS -> {
                if (request.getSelectedUserIds() == null || request.getSelectedUserIds().isEmpty()) {
                    throw new RuntimeException("Selected users are required for SELECTED_USERS assignment");
                }

                return request.getSelectedUserIds()
                        .stream()
                        .map(userId -> userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)))
                        .toList();
            }

            case TEAM_MEMBERS -> {
                if (request.getTeamId() == null) {
                    throw new RuntimeException("Team ID is required for TEAM_MEMBERS assignment");
                }

                List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(request.getTeamId());

                return teamMembers.stream()
                        .map(TeamMember::getUser)
                        .toList();
            }

            case SQUAD_PLAYERS -> throw new RuntimeException("SQUAD_PLAYERS assignment will be added in the next step");

            case GOING_USERS -> throw new RuntimeException("GOING_USERS assignment will be added in the next step");

            default -> throw new RuntimeException("Unsupported assignment type");
        }
    }

    /**
     * Maps master fee definition entity to response DTO.
     */
    private FeeDefinitionResponse mapFeeDefinitionToResponse(FeeDefinition feeDefinition) {
        return new FeeDefinitionResponse(
                feeDefinition.getId(),
                feeDefinition.getTitle(),
                feeDefinition.getFeeType(),
                feeDefinition.getAmount(),
                feeDefinition.getDueDate(),
                feeDefinition.getDescription(),
                feeDefinition.getMatchId(),
                feeDefinition.getEventId(),
                feeDefinition.getTeamId(),
                feeDefinition.getSeason(),
                feeDefinition.getAssignmentType(),
                feeDefinition.getCreatedBy(),
                feeDefinition.getCreatedAt(),
                feeDefinition.isActive()
        );
    }

    /**
     * Maps user-level fee assignment entity to response DTO.
     */
    private FeeAssignmentResponse mapFeeAssignmentToResponse(FeeAssignment assignment) {
        FeeDefinition feeDefinition = assignment.getFeeDefinition();

        return new FeeAssignmentResponse(
                assignment.getId(),
                feeDefinition.getId(),
                assignment.getUser().getId(),
                assignment.getUser().getFullName(),
                feeDefinition.getTitle(),
                feeDefinition.getFeeType(),
                assignment.getAmount(),
                assignment.getDueDate(),
                feeDefinition.getDescription(),
                feeDefinition.getMatchId(),
                feeDefinition.getEventId(),
                feeDefinition.getTeamId(),
                feeDefinition.getSeason(),
                assignment.getStatus(),
                assignment.getPaymentMethod(),
                assignment.getPaymentNote(),
                assignment.getAssignedAt(),
                assignment.getSubmittedAt(),
                assignment.getConfirmedAt(),
                assignment.getConfirmedBy(),
                assignment.getWaivedAt(),
                assignment.getWaiverReason(),
                assignment.getLastReminderSentAt(),
                assignment.getReminderCount()
        );
    }


    /**
     * Player submits payment note for one fee assignment.
     *
     * Important:
     * We do not mark it PAID directly.
     * Instead we move it to PAYMENT_SUBMITTED.
     *
     * Admin/captain will confirm later.
     */
    @Transactional
    public String submitMyPayment(Long assignmentId, String email, String paymentMethod, String paymentNote) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FeeAssignment assignment = feeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Fee assignment not found"));

        // Make sure user can only submit payment for their own fee row
        if (!assignment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only submit payment for your own fee");
        }

        // Do not allow submission if already paid or waived
        if (assignment.getStatus() == FeeStatus.PAID) {
            throw new RuntimeException("This fee is already marked as paid");
        }

        if (assignment.getStatus() == FeeStatus.WAIVED) {
            throw new RuntimeException("This fee has already been waived");
        }

        assignment.setPaymentMethod(paymentMethod);
        assignment.setPaymentNote(paymentNote);
        assignment.setSubmittedAt(LocalDateTime.now());
        assignment.setStatus(FeeStatus.PAYMENT_SUBMITTED);

        feeAssignmentRepository.save(assignment);

        return "Payment submitted successfully. Waiting for admin/captain confirmation.";
    }

    /**
     * Admin/captain confirms payment for one assignment.
     */
    @Transactional
    public String confirmFeePayment(Long assignmentId, String email) {
        User approver = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FeeAssignment assignment = feeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Fee assignment not found"));

        if (assignment.getStatus() == FeeStatus.WAIVED) {
            throw new RuntimeException("Cannot confirm payment for a waived fee");
        }

        assignment.setStatus(FeeStatus.PAID);
        assignment.setConfirmedAt(LocalDateTime.now());
        assignment.setConfirmedBy(approver.getFullName());

        feeAssignmentRepository.save(assignment);

        return "Fee marked as paid successfully";
    }

    /**
     * Admin/captain waives a fee assignment.
     */
    @Transactional
    public String waiveFee(Long assignmentId, String waiverReason) {
        FeeAssignment assignment = feeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Fee assignment not found"));

        if (assignment.getStatus() == FeeStatus.PAID) {
            throw new RuntimeException("Cannot waive a fee that is already paid");
        }

        assignment.setStatus(FeeStatus.WAIVED);
        assignment.setWaivedAt(LocalDateTime.now());
        assignment.setWaiverReason(waiverReason);

        feeAssignmentRepository.save(assignment);

        return "Fee waived successfully";
    }
}