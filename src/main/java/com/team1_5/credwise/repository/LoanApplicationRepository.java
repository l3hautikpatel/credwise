package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Loan Application entities
 *
 * Provides database operations and custom query methods for loan applications
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    /**
     * Find loan applications by user ID
     *
     * @param userId Unique identifier of the user
     * @return List of loan applications for the user
     */
    List<LoanApplication> findByUser_Id(Long userId);

    /**
     * Find loan applications by user ID and specific status
     *
     * @param userId Unique identifier of the user
     * @param status Status of the loan application
     * @return List of loan applications matching the criteria
     */
    List<LoanApplication> findByUser_IdAndStatus(
            Long userId,
            LoanApplication.ApplicationStatus status
    );

    /**
     * Find a specific loan application for a user
     *
     * @param applicationId Unique identifier of the loan application
     * @param userId Unique identifier of the user
     * @return Optional containing the loan application
     */
    Optional<LoanApplication> findByApplicationIdAndUser_Id(
            String applicationId,
            Long userId
    );

    /**
     * Count active loan applications for a user
     *
     * @param userId Unique identifier of the user
     * @param statuses List of statuses to consider as active
     * @return Number of active loan applications
     */
    long countByUser_IdAndStatusIn(
            Long userId,
            List<LoanApplication.ApplicationStatus> statuses
    );

    /**
     * Find recent loan applications for a user
     *
     * @param userId Unique identifier of the user
//     * @param limit Maximum number of applications to retrieve
     * @return List of recent loan applications
     */
    @Query("SELECT la FROM LoanApplication la " +
            "WHERE la.user.id = :userId " +
            "ORDER BY la.createdAt DESC")
    List<LoanApplication> findRecentApplications(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find loan applications created within a specific time range
     *
     * @param startDate Beginning of the time range
     * @param endDate End of the time range
     * @return List of loan applications within the specified time range
     */
    List<LoanApplication> findByCreatedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Check if a user has an existing application with specific statuses
     *
     * @param userId Unique identifier of the user
     * @param statuses List of statuses to check
     * @return Boolean indicating existence of applications
     */
    boolean existsByUser_IdAndStatusIn(
            Long userId,
            List<LoanApplication.ApplicationStatus> statuses
    );

    /**
     * Find loan applications by credit score range
     *
     * @param minScore Minimum credit score
     * @param maxScore Maximum credit score
     * @return List of loan applications within the credit score range
     */
    @Query("SELECT la FROM LoanApplication la " +
            "WHERE la.financialInfo.creditScore BETWEEN :minScore AND :maxScore")
    List<LoanApplication> findByCreditScoreRange(
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore
    );

    /**
     * Custom method to find applications by multiple criteria
     *
     * @param userId User ID
     * @param status Application status
     * @param minAmount Minimum loan amount
     * @return List of filtered loan applications
     */
    @Query("SELECT la FROM LoanApplication la " +
            "WHERE (:userId IS NULL OR la.user.id = :userId) " +
            "AND (:status IS NULL OR la.status = :status) " +
            "AND (:minAmount IS NULL OR la.loanDetails.requestedAmount >= :minAmount)")
    List<LoanApplication> findByMultipleCriteria(
            @Param("userId") Long userId,
            @Param("status") LoanApplication.ApplicationStatus status,
            @Param("minAmount") BigDecimal minAmount
    );
}