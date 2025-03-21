package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.CreditScoreDetails;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.util.MLModelIntegration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanApplicationWorkflowService {
    private final LoanApplicationRepository loanApplicationRepository;
    private final MLModelIntegration mlModelIntegration;
    private final CreditScoreService creditScoreService;
    private final AuditLogService auditLogService;

    public LoanApplicationWorkflowService(
            LoanApplicationRepository loanApplicationRepository,
            MLModelIntegration mlModelIntegration,
            CreditScoreService creditScoreService,
            AuditLogService auditLogService
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.mlModelIntegration = mlModelIntegration;
        this.creditScoreService = creditScoreService;
        this.auditLogService = auditLogService;
    }

    /**
     * Process initial loan application submission
     *
     * @param loanApplication Loan application to process
     * @return Processed loan application
     */
    @Transactional
    public LoanApplication submitInitialApplication(LoanApplication loanApplication) {
        // Validate initial application
        validateInitialApplication(loanApplication);

        // Set initial status
        loanApplication.setStatus(LoanApplication.ApplicationStatus.SUBMITTED);

        // Save initial application
        loanApplicationRepository.save(loanApplication);

        // Trigger initial processing
        processInitialEligibility(loanApplication);

        return loanApplication;
    }

    /**
     * Validate initial loan application
     *
     * @param loanApplication Loan application to validate
     */
    private void validateInitialApplication(LoanApplication loanApplication) {
        // Perform initial validation checks
        // 1. Check loan amount limits
        // 2. Validate user information
        // 3. Basic eligibility criteria
    }

    /**
     * Process initial loan eligibility
     *
     * @param loanApplication Loan application to process
     */
    private void processInitialEligibility(LoanApplication loanApplication) {
        try {
            // Check credit score
            boolean creditEligible = creditScoreService.checkEligibility(
                    loanApplication.getFinancialInfo().getCreditScore()
            );

            // Run ML model prediction
            var mlDecision = mlModelIntegration.predictLoanEligibility(
                    prepareMlInput(loanApplication)
            );

            // Update application status based on eligibility
            if (creditEligible && mlDecision.isApproved()) {
                loanApplication.setStatus(LoanApplication.ApplicationStatus.UNDER_REVIEW);
                loanApplication.setApprovalProbability(mlDecision.getApprovalProbability());
            } else {
                loanApplication.setStatus(LoanApplication.ApplicationStatus.REJECTED);
            }

            // Save updated application
            loanApplicationRepository.save(loanApplication);

            // Log processing event
            auditLogService.logEvent(
                    AuditLogService.AuditEventType.LOAN_APPLICATION_SUBMITTED,
                    "Initial loan application processed",
                    Map.of(
                            "applicationId", loanApplication.getApplicationId(),
                            "status", loanApplication.getStatus(),
                            "approvalProbability", loanApplication.getApprovalProbability()
                    )
            );

        } catch (Exception e) {
            // Handle processing errors
            loanApplication.setStatus(LoanApplication.ApplicationStatus.REQUIRES_MORE_INFO);
            loanApplicationRepository.save(loanApplication);

            auditLogService.logSystemError(
                    "Error processing loan application",
                    e
            );
        }
    }

    /**
     * Prepare input for ML model
     *
     * @param loanApplication Loan application
     * @return Prepared input map
     */
    private Map<String, Object> prepareMlInput(LoanApplication loanApplication) {
        Map<String, Object> mlInput = new HashMap<>();

        // Populate ML input with relevant application details
        mlInput.put("credit_score", loanApplication.getFinancialInfo().getCreditScore());
        mlInput.put("annual_income", calculateAnnualIncome(loanApplication));
        mlInput.put("requested_amount", loanApplication.getLoanDetails().getRequestedAmount());
        // Add more relevant fields

        return mlInput;
    }

    /**
     * Calculate annual income
     *
     * @param loanApplication Loan application
     * @return Annual income
     */
    private BigDecimal calculateAnnualIncome(LoanApplication loanApplication) {
        return loanApplication.getFinancialInfo().getMonthlyIncome()
                .multiply(BigDecimal.valueOf(12));
    }

    /**
     * Request additional information
     *
     * @param applicationId Loan application ID
     * @param requiredDocuments List of required additional documents
     */
    @Transactional
    public void requestAdditionalInformation(
            String applicationId,
            List<String> requiredDocuments
    ) {
        LoanApplication application = loanApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new LoanApplicationException("Application not found"));

        application.setStatus(LoanApplication.ApplicationStatus.REQUIRES_MORE_INFO);

        // Store required documents
        application.setRequiredAdditionalDocuments(requiredDocuments);

        loanApplicationRepository.save(application);

        // Trigger notification to user
//        notificationService.sendAdditionalInfoRequest(
//                application.getUser().getEmail(),
//                requiredDocuments
//        );
    }
}
