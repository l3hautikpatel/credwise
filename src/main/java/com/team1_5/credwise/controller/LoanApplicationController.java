package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.service.LoanApplicationService;
import com.team1_5.credwise.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Loan Application Management
 *
 * Provides endpoints for submitting, retrieving, and managing loan applications
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/loan-applications")
@Tag(name = "Loan Application", description = "Loan Application Management APIs")
public class LoanApplicationController {
    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationController.class);

    private final LoanApplicationService loanApplicationService;
    private final JwtUtil jwtUtil;

    public LoanApplicationController(
            LoanApplicationService loanApplicationService,
            JwtUtil jwtUtil
    ) {
        this.loanApplicationService = loanApplicationService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Submit a new loan application
     *
     * @param request Loan application details
     * @param authHeader JWT authentication token
     * @return Loan application submission response
     */
    @PostMapping
    @Operation(
            summary = "Submit a new loan application",
            description = "Submit a comprehensive loan application with personal, financial, and document details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Loan application submitted successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoanApplicationResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized access",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<LoanApplicationResponse> submitLoanApplication(
            @Valid
            @RequestBody
            @Parameter(
                    description = "Loan application details",
                    required = true
            ) LoanApplicationRequest request,
            @RequestHeader("Authorization")
            @Parameter(
                    description = "JWT Authentication Token",
                    required = true,
                    example = "Bearer your_jwt_token_here"
            ) String authHeader
    ) {
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(authHeader);

            // Submit loan application
            LoanApplicationResponse response = loanApplicationService
                    .submitLoanApplication(request, userId);

            logger.info("Loan application submitted successfully for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error submitting loan application", e);
            throw e;
        }
    }

    /**
     * Retrieve loan application history for the authenticated user
     *
     * @param authHeader JWT authentication token
     * @return List of loan applications
     */
    @GetMapping("/history")
    @Operation(
            summary = "Get loan application history",
            description = "Retrieve all loan applications for the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved loan application history",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoanApplicationResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized access",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<List<LoanApplicationResponse>> getLoanApplicationHistory(
            @RequestHeader("Authorization")
            @Parameter(
                    description = "JWT Authentication Token",
                    required = true,
                    example = "Bearer your_jwt_token_here"
            ) String authHeader
    ) {
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(authHeader);

            // Retrieve loan application history
            List<LoanApplicationResponse> applications = loanApplicationService
                    .getUserLoanApplications(userId);

            logger.info("Retrieved loan application history for user: {}", userId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            logger.error("Error retrieving loan application history", e);
            throw e;
        }
    }

    /**
     * Retrieve details of a specific loan application
     *
     * @param applicationId Unique identifier of the loan application
     * @param authHeader JWT authentication token
     * @return Loan application details
     */
    @GetMapping("/{applicationId}")
    @Operation(
            summary = "Get loan application details",
            description = "Retrieve details of a specific loan application",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved loan application details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoanApplicationResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized access",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Loan application not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<LoanApplicationResponse> getLoanApplicationDetails(
            @PathVariable
            @Parameter(
                    description = "Unique identifier of the loan application",
                    required = true
            ) String applicationId,
            @RequestHeader("Authorization")
            @Parameter(
                    description = "JWT Authentication Token",
                    required = true,
                    example = "Bearer your_jwt_token_here"
            ) String authHeader
    ) {
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(authHeader);

            // Retrieve specific loan application details
            LoanApplicationResponse application = loanApplicationService
                    .getLoanApplicationDetails(applicationId, userId);

            logger.info("Retrieved loan application details for user: {}, application: {}",
                    userId, applicationId);
            return ResponseEntity.ok(application);
        } catch (Exception e) {
            logger.error("Error retrieving loan application details", e);
            throw e;
        }
    }

    /**
     * Update the status of a loan application
     *
     * @param applicationId Unique identifier of the loan application
     * @param status New status for the application
     * @param authHeader JWT authentication token
     * @return Updated loan application response
     */
    @PutMapping("/{applicationId}/status")
    @Operation(
            summary = "Update loan application status",
            description = "Update the status of a specific loan application",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated loan application status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoanApplicationResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized access",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Loan application not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<LoanApplicationResponse> updateLoanApplicationStatus(
            @PathVariable
            @Parameter(
                    description = "Unique identifier of the loan application",
                    required = true
            ) String applicationId,
            @RequestParam
            @Parameter(
                    description = "New status for the loan application",
                    required = true,
                    schema = @Schema(implementation = LoanApplication.ApplicationStatus.class)
            ) LoanApplication.ApplicationStatus status,
            @RequestHeader("Authorization")
            @Parameter(
                    description = "JWT Authentication Token",
                    required = true,
                    example = "Bearer your_jwt_token_here"
            ) String authHeader
    ) {
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(authHeader);

            // Update loan application status
            LoanApplicationResponse response = loanApplicationService
                    .updateLoanApplicationStatus(applicationId, userId, status);

            logger.info("Updated loan application status for user: {}, application: {}, new status: {}",
                    userId, applicationId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating loan application status", e);
            throw e;
        }
    }

    /**
     * Extract user ID from JWT token
     *
     * @param authHeader Authorization header containing JWT token
     * @return User ID extracted from the token
     */
    private Long extractUserIdFromToken(String authHeader) {
        // Remove "Bearer " prefix
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}