package com.team1_5.credwise.service;

import com.team1_5.credwise.config.CreditScoreValidator;
import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.exception.UserNotFoundException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.UserRepository;
import com.team1_5.credwise.util.MLModelIntegration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Loan Application processing
 *
 * Handles business logic for loan application submission, validation, and processing
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Service
public class LoanApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationService.class);

    // Configurable maximum number of applications
    @Value("${loan.max.applications:1000000}")
    private int MAX_APPLICATIONS;

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;
    private final CreditScoreValidator creditScoreValidator;
    private final MLModelIntegration mlModelIntegration;

    public LoanApplicationService(
            LoanApplicationRepository loanApplicationRepository,
            UserRepository userRepository,
            CreditScoreValidator creditScoreValidator,
            MLModelIntegration mlModelIntegration
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.userRepository = userRepository;
        this.creditScoreValidator = creditScoreValidator;
        this.mlModelIntegration = mlModelIntegration;
    }

    /**
     * Submit a new loan application
     *
     * @param request Loan application request details
     * @param userId ID of the user submitting the application
     * @return Loan application response
     */
    @Transactional
    public LoanApplicationResponse submitLoanApplication(
            LoanApplicationRequest request,
            Long userId
    ) {
        // Validate user
        User applicant = validateUser(userId);

        // Check application limits
        validateApplicationLimit(userId);

        // Convert request to entity
        LoanApplication loanApplication = convertToEntity(request, applicant);

        // Validate credit score (placeholder)
        validateCreditScore(request);

        // Process documents
        processDocuments(loanApplication, request.getDocuments());

        // Integrate with ML model for decision
        LoanApplication.ApplicationStatus status = processMLModelDecision(loanApplication);
        loanApplication.setStatus(status);

        // Save application
        loanApplication = loanApplicationRepository.save(loanApplication);

        // Prepare and return response
        return prepareLoanApplicationResponse(loanApplication);
    }

    /**
     * Validate user existence
     *
     * @param userId User ID to validate
     * @return User entity
     * @throws UserNotFoundException if user not found
     */
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });
    }

    /**
     * Validate application submission limits
     *
     * @param userId User ID to check applications for
     * @throws LoanApplicationException if limit exceeded
     */
    private void validateApplicationLimit(Long userId) {
        List<LoanApplication.ApplicationStatus> activeStatuses = Arrays.asList(
                LoanApplication.ApplicationStatus.DRAFT,
                LoanApplication.ApplicationStatus.SUBMITTED,
                LoanApplication.ApplicationStatus.UNDER_REVIEW
        );

        long activeApplicationCount = loanApplicationRepository
                .countByUser_IdAndStatusIn(userId, activeStatuses);

        if (activeApplicationCount >= MAX_APPLICATIONS) {
            logger.warn("Application limit exceeded for user: {}", userId);
            throw new LoanApplicationException(
                    "Maximum number of active applications reached: " + MAX_APPLICATIONS
            );
        }
    }

    /**
     * Convert request DTO to entity
     *
     * @param request Loan application request
     * @param applicant User submitting the application
     * @return LoanApplication entity
     */
    private LoanApplication convertToEntity(
            LoanApplicationRequest request,
            User applicant
    ) {
        LoanApplication loanApplication = new LoanApplication();

        // Map loan details
        LoanApplication.LoanDetails loanDetails = new LoanApplication.LoanDetails();
        loanDetails.setProductType(request.getLoanDetails().getProductType());
        loanDetails.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
        loanDetails.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
        loanDetails.setRequestedTerm(request.getLoanDetails().getRequestedTerm());
        loanApplication.setLoanDetails(loanDetails);

        // Map personal info
        // (Similar mapping for other fields)

        // Set user and timestamps
        loanApplication.setUser(applicant);
        loanApplication.setCreatedAt(LocalDateTime.now());

        return loanApplication;
    }

    /**
     * Validate credit score
     *
     * @param request Loan application request
     */
    private void validateCreditScore(LoanApplicationRequest request) {
        // Placeholder for credit score validation
        // In future, implement comprehensive validation
        boolean isValidCreditScore = creditScoreValidator.validateCreditScore(
                convertToCreditScoreDetails(request)
        );

        if (!isValidCreditScore) {
            logger.warn("Credit score validation failed");
            // Optionally throw an exception or log a warning
        }
    }

    /**
     * Process documents for the loan application
     *
     * @param loanApplication Loan application entity
     * @param documents List of documents
     */
    private void processDocuments(
            LoanApplication loanApplication,
            List<LoanApplicationRequest.DocumentUpload> documents
    ) {
        if (documents != null && !documents.isEmpty()) {
            List<LoanApplication.DocumentUpload> processedDocs = documents.stream()
                    .map(doc -> {
                        LoanApplication.DocumentUpload uploadDoc =
                                new LoanApplication.DocumentUpload();

                        try {
                            uploadDoc.setDocumentType(doc.getDocumentType());
                            uploadDoc.setFileContent(Base64.getDecoder().decode(doc.getFile()));
                            uploadDoc.setFileName("uploaded_document");
                            uploadDoc.setFileType("application/octet-stream");

                            return uploadDoc;
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid document encoding: {}", doc.getDocumentType());
                            throw new LoanApplicationException(
                                    "Invalid document encoding for " + doc.getDocumentType()
                            );
                        }
                    })
                    .collect(Collectors.toList());

            loanApplication.setDocuments(processedDocs);
        }
    }

    /**
     * Process ML model decision
     *
     * @param loanApplication Loan application entity
     * @return Application status based on ML model decision
     */
    private LoanApplication.ApplicationStatus processMLModelDecision(
            LoanApplication loanApplication
    ) {
        // Prepare input for ML model
        var mlInput = prepareMLModelInput(loanApplication);

        // Get ML model decision
        var mlDecision = mlModelIntegration.predictLoanEligibility(mlInput);

        // Determine application status based on ML decision
        return mlDecision.isApproved()
                ? LoanApplication.ApplicationStatus.APPROVED
                : LoanApplication.ApplicationStatus.REJECTED;
    }

    /**
     * Prepare response for loan application
     *
     * @param loanApplication Processed loan application
     * @return Loan application response
     */
    private LoanApplicationResponse prepareLoanApplicationResponse(
            LoanApplication loanApplication
    ) {
        return LoanApplicationResponse.builder()
                .applicationId(loanApplication.getApplicationId())
                .message("Loan application processed successfully")
                .status(loanApplication.getStatus().name())
                .build();
    }

    // Additional methods for retrieving, updating applications would be added here
}