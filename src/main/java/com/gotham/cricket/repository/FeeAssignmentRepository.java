package com.gotham.cricket.repository;

import com.gotham.cricket.entity.FeeAssignment;
import com.gotham.cricket.entity.FeeDefinition;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for user-level fee assignments.
 */
public interface FeeAssignmentRepository extends JpaRepository<FeeAssignment, Long> {

    /**
     * Find all fee assignments for one user.
     * Useful for My Fees screen.
     */
    List<FeeAssignment> findByUserOrderByDueDateAsc(User user);

    /**
     * Find all fee assignments for one fee definition.
     * Useful for admin fee details screen.
     */
    List<FeeAssignment> findByFeeDefinitionOrderByDueDateAsc(FeeDefinition feeDefinition);

    /**
     * Find only unpaid assignments for one user.
     */
    List<FeeAssignment> findByUserAndStatusOrderByDueDateAsc(User user, FeeStatus status);

    /**
     * Find all overdue unpaid assignments.
     * Useful for reminder jobs later.
     */
    List<FeeAssignment> findByStatusAndDueDateBefore(FeeStatus status, LocalDateTime dueDate);

    /**
     * Find all assignments for one user by status list can be added later if needed.
     */
    long countByUserAndStatus(User user, FeeStatus status);
}