package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.model.*;
import com.team1_5.credwise.repository.*;
import com.team1_5.credwise.util.CreditScoreService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository loanAppRepo;
    private final PersonalInfoRepository personalInfoRepo;
    private final AddressRepository addressRepo;
    private final FinancialInfoRepository financialInfoRepo;
    private final EmploymentHistoryRepository employmentRepo;
    private final DebtRepository debtRepo;
    private final AssetRepository assetRepo;
    private final DocumentRepository documentRepo;
    private final CreditScoreService creditScoreService;
    private final UserRepository userRepo;

    public LoanApplicationService(LoanApplicationRepository loanAppRepo,
                                  PersonalInfoRepository personalInfoRepo,
                                  AddressRepository addressRepo,
                                  FinancialInfoRepository financialInfoRepo,
                                  EmploymentHistoryRepository employmentRepo,
                                  DebtRepository debtRepo,
                                  AssetRepository assetRepo,
                                  DocumentRepository documentRepo,
                                  CreditScoreService creditScoreService,
                                  UserRepository userRepo) {
        this.loanAppRepo = loanAppRepo;
        this.personalInfoRepo = personalInfoRepo;
        this.addressRepo = addressRepo;
        this.financialInfoRepo = financialInfoRepo;
        this.employmentRepo = employmentRepo;
        this.debtRepo = debtRepo;
        this.assetRepo = assetRepo;
        this.documentRepo = documentRepo;
        this.creditScoreService = creditScoreService;
        this.userRepo = userRepo;
    }

    public LoanApplicationResponse processLoanApplication(Long userId, LoanApplicationRequest request) {
        validateRequest(request);

        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new LoanApplicationException("User not found", HttpStatus.NOT_FOUND));

            // 1. Save base application
            LoanApplication application = saveLoanApplication(user, request);

            // 2. Save personal information
            savePersonalInformation(application, request.getPersonalInformation());

            // 3. Save financial information
            FinancialInfo financialInfo = saveFinancialInformation(application, request.getFinancialInformation());

            // 4. Prepare and calculate credit score
            Map<String, Object> creditData = prepareCreditData(request, financialInfo);
            double creditScore = creditScoreService.calculateCreditScore(creditData);

            // 5. Update application status
            updateApplicationStatus(application, creditScore);

            // 6. Save documents
            saveApplicationDocuments(application, request.getDocuments());

            return buildSuccessResponse(application);

        } catch (LoanApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new LoanApplicationException("Processing failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRequest(LoanApplicationRequest request) {
        if (request.getLoanDetails() == null ||
                request.getPersonalInformation() == null ||
                request.getFinancialInformation() == null) {
            throw new LoanApplicationException("Invalid request structure", HttpStatus.BAD_REQUEST);
        }

        if (request.getLoanDetails().getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LoanApplicationException("Requested amount must be positive", HttpStatus.BAD_REQUEST);
        }
    }

    private LoanApplication saveLoanApplication(User user, LoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();
        application.setUser(user);
        application.setProductType(request.getLoanDetails().getProductType());
        application.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
        application.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
        application.setRequestedTermMonths(request.getLoanDetails().getRequestedTermMonths());
        application.setStatus("RECEIVED");
        return loanAppRepo.save(application);
    }

    private void savePersonalInformation(LoanApplication application,
                                         LoanApplicationRequest.PersonalInformation personalInfoDto) {
        // Save address first
        Address address = new Address();
        address.setStreetAddress(personalInfoDto.getCurrentAddress().getStreetAddress());
        address.setCity(personalInfoDto.getCurrentAddress().getCity());
        address.setProvince(personalInfoDto.getCurrentAddress().getProvince());
        address.setPostalCode(personalInfoDto.getCurrentAddress().getPostalCode());
        address.setCountry(personalInfoDto.getCurrentAddress().getCountry());
        address.setDurationMonths(personalInfoDto.getCurrentAddress().getDurationAtAddressMonths());
        Address savedAddress = addressRepo.save(address);

        // Save personal info
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setLoanApplication(application);
        personalInfo.setFirstName(personalInfoDto.getFirstName());
        personalInfo.setLastName(personalInfoDto.getLastName());
        personalInfo.setEmail(personalInfoDto.getEmailAddress());
        personalInfo.setPhoneNumber(personalInfoDto.getPhoneNumber());
        personalInfo.setDateOfBirth(personalInfoDto.getDateOfBirth());
        personalInfo.setAddress(savedAddress);
        personalInfoRepo.save(personalInfo);
    }

    private FinancialInfo saveFinancialInformation(LoanApplication application,
                                                   LoanApplicationRequest.FinancialInformation financialInfoDto) {
        // Save main financial info
        FinancialInfo financialInfo = new FinancialInfo();
        financialInfo.setLoanApplication(application);
        financialInfo.setMonthlyIncome(financialInfoDto.getMonthlyIncome());
        financialInfo.setMonthlyExpenses(financialInfoDto.getMonthlyExpenses());
        financialInfo.setCreditScore(financialInfoDto.getCreditScore());
        financialInfo.setCurrentCreditLimit(financialInfoDto.getCurrentCreditLimit());
        financialInfo.setCreditTotalUsage(financialInfoDto.getCreditTotalUsage());
        FinancialInfo savedFinancialInfo = financialInfoRepo.save(financialInfo);

        // Save employment history
        saveEmploymentHistory(savedFinancialInfo, financialInfoDto.getEmploymentDetails());

        // Save debts
        saveDebts(savedFinancialInfo, financialInfoDto.getExistingDebts());

        // Save assets
        saveAssets(savedFinancialInfo, financialInfoDto.getAssets());

        return savedFinancialInfo;
    }

    private void saveEmploymentHistory(
            FinancialInfo financialInfo,
            List<LoanApplicationRequest.FinancialInformation.EmploymentDetail> employmentDetails
    )  {
        List<EmploymentHistory> histories = employmentDetails.stream()
                .map(dto -> {
                    EmploymentHistory history = new EmploymentHistory();
                    history.setFinancialInfo(financialInfo);
                    history.setEmployerName(dto.getEmployerName());
                    history.setPosition(dto.getPosition());
                    history.setStartDate(dto.getStartDate());
                    history.setEndDate(dto.getEndDate());
                    history.setEmploymentType(dto.getEmploymentType());
                    history.setDurationMonths(dto.getEmploymentDurationMonths());
                    return history;
                })
                .toList();
        employmentRepo.saveAll(histories);
    }

    private void saveDebts(FinancialInfo financialInfo,
                           List<LoanApplicationRequest.FinancialInformation.ExistingDebt> debts) {
        List<Debt> debtEntities = debts.stream()
                .map(dto -> {
                    Debt debt = new Debt();
                    debt.setFinancialInfo(financialInfo);
                    debt.setDebtType(dto.getDebtType());
                    debt.setOutstandingAmount(dto.getOutstandingAmount());
                    debt.setInterestRate(dto.getInterestRate());
                    debt.setMonthlyPayment(dto.getMonthlyPayment());
                    debt.setRemainingTermMonths(dto.getRemainingTermMonths());
                    debt.setLender(dto.getLender());
                    debt.setPaymentHistory(dto.getPaymentHistory());
                    return debt;
                })
                .toList();
        debtRepo.saveAll(debtEntities);
    }

    private void saveAssets(FinancialInfo financialInfo,
                            List<LoanApplicationRequest.FinancialInformation.Asset> assets) {
        List<Asset> assetEntities = assets.stream()
                .map(dto -> {
                    Asset asset = new Asset();
                    asset.setFinancialInfo(financialInfo);
                    asset.setAssetType(dto.getAssetType());
                    asset.setDescription(dto.getDescription());
                    asset.setEstimatedValue(dto.getEstimatedValue());
                    return asset;
                })
                .toList();
        assetRepo.saveAll(assetEntities);
    }

    private void saveApplicationDocuments(LoanApplication application,
                                          List<LoanApplicationRequest.DocumentRequest> documents) {
        List<Document> documentEntities = documents.stream()
                .map(dto -> {
                    validateDocument(dto);
                    Document doc = new Document();
                    doc.setLoanApplication(application);
                    doc.setDocumentType(dto.getDocumentType());
                    doc.setFileData(dto.getFile());
                    return doc;
                })
                .toList();
        documentRepo.saveAll(documentEntities);
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            throw new LoanApplicationException("Invalid date format. Use YYYY-MM-DD", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDocument(LoanApplicationRequest.DocumentRequest doc) {
        if (doc.getFile() == null || doc.getFile().isEmpty()) {
            throw new LoanApplicationException("Document file cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (doc.getDocumentType() == null || doc.getDocumentType().isEmpty()) {
            throw new LoanApplicationException("Document type is required", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> prepareCreditData(LoanApplicationRequest request, FinancialInfo financialInfo) {
        // Implement credit data preparation logic here
        // Return map of parameters needed for credit score calculation
        return Map.of();
    }

    private void updateApplicationStatus(LoanApplication application, double creditScore) {
        application.setCreditScore(creditScore);
        application.setStatus(creditScore >= 650 ? "APPROVED" : "DENIED");
        loanAppRepo.save(application);
    }

    private LoanApplicationResponse buildSuccessResponse(LoanApplication application) {
        return new LoanApplicationResponse(
                application.getId().toString(),
                "Loan application processed successfully",
                application.getStatus()
        );
    }
}