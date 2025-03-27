package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.dto.LoanResultDTO;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.model.*;
import com.team1_5.credwise.repository.*;
import com.team1_5.credwise.util.CreditScoreValidator;
import com.team1_5.credwise.util.DummyLoanResultGenerator;
import com.team1_5.credwise.util.LoanResultCalculator;

//import jakarta.transaction.Transactional;
import com.team1_5.credwise.util.LoanResultGenerator;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository loanAppRepo;
    private final AddressRepository addressRepo;
    private final FinancialInfoRepository financialInfoRepo;
    private final DebtRepository debtRepo;
    private final AssetRepository assetRepo;
    private final DocumentRepository documentRepo;
    private final CreditScoreValidator creditValidator;
    private final LoanResultCalculator resultCalculator;
    private final UserRepository userRepo;
    private final PersonalInfoRepository personalInfoRepo;
    private final LoanApplicationResultRepository loanApplicationResultRepo;
    private final LoanResultGenerator loanResultGenerator;
    private final DummyLoanResultGenerator resultGenerator;
    private final FinancialSummaryRepository financialSummaryRepo;
    private final EmploymentHistoryRepository employmentHistoryRepo;



    public LoanApplicationService(LoanApplicationRepository loanAppRepo,
                                  AddressRepository addressRepo,
                                  FinancialInfoRepository financialInfoRepo,
                                  DebtRepository debtRepo,
                                  AssetRepository assetRepo,
                                  DocumentRepository documentRepo,
                                  CreditScoreValidator creditValidator,
                                  LoanResultCalculator resultCalculator,
                                  UserRepository userRepo, PersonalInfoRepository personalInfoRepo,
                                  LoanApplicationResultRepository loanApplicationResultRepo,
                                  LoanResultGenerator loanResultGenerator,
                                  DummyLoanResultGenerator resultGenerator,
                                  FinancialSummaryRepository financialSummaryRepo,
                                  EmploymentHistoryRepository employmentHistoryRepo) {
        this.loanAppRepo = loanAppRepo;
        this.addressRepo = addressRepo;
        this.financialInfoRepo = financialInfoRepo;
        this.debtRepo = debtRepo;
        this.assetRepo = assetRepo;
        this.documentRepo = documentRepo;
        this.creditValidator = creditValidator;
        this.resultCalculator = resultCalculator;
        this.userRepo = userRepo;
        this.personalInfoRepo = personalInfoRepo;
        this.loanApplicationResultRepo = loanApplicationResultRepo;
        this.loanResultGenerator = loanResultGenerator;
        this.resultGenerator = resultGenerator;
        this.financialSummaryRepo = financialSummaryRepo;
        this.employmentHistoryRepo = employmentHistoryRepo;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoanApplicationResponse processLoanApplication(Long userId, LoanApplicationRequest request) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new LoanApplicationException("User not found with ID: " + userId));

//            LoanApplication application = saveLoanApplication(user, request);
//            boolean isCreditValid = creditValidator.validate(request);
//            boolean isApproved = resultCalculator.calculateResult(request, isCreditValid);
//            saveApplicationResult(application, isApproved);
//
//            return new LoanApplicationResponse(
//                    application.getId().toString(),
//                    "Loan application submitted successfully",
//                    isApproved ? "APPROVED" : "UNDER_REVIEW"
//            );



            LoanApplication application = saveLoanApplication(user, request);
            LoanResultDTO result = resultGenerator.generateDummyResult();

            saveFinancialSummary(user, request);
            saveApplicationResult(application, result);

            return new LoanApplicationResponse(
                    application.getId().toString(),
                    "Loan application processed with dummy data",
                    "APPROVED"
            );
        } catch (DateTimeParseException e) {
            throw new LoanApplicationException("Invalid date format. Use YYYY-MM-DD");
        }
    }

    private LoanApplication saveLoanApplication(User user, LoanApplicationRequest request) {
        LoanApplication application = createAndSaveLoanApplication(user, request);
        saveAddressAndPersonalInfo(request, application);
        saveFinancialInformation(request, application);
        saveDocuments(application, request.getDocuments());
        return application;
    }

    private LoanApplication createAndSaveLoanApplication(User user, LoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();
        application.setUser(user);
        application.setProductType(request.getLoanDetails().getProductType());
        application.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
        application.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
        application.setRequestedTerm(request.getLoanDetails().getRequestedTerm());
        application.setStatus("PENDING");
        return loanAppRepo.save(application);
    }

    private void saveAddressAndPersonalInfo(LoanApplicationRequest request, LoanApplication application) {
        Address address = saveAddress(request.getPersonalInfo().getAddress());
        savePersonalInfo(request.getPersonalInfo(), address, application);
    }

    private Address saveAddress(LoanApplicationRequest.Address addressDto) {
        Address address = new Address();
        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setZipCode(addressDto.getZipCode());
        address.setCountry(addressDto.getCountry());
        address.setDuration(addressDto.getDuration());
        return addressRepo.save(address);
    }

    private void savePersonalInfo(LoanApplicationRequest.PersonalInfo personalInfoDto,
                                  Address address, LoanApplication application) {
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setLoanApplication(application);
        personalInfo.setFirstName(personalInfoDto.getFirstName());
        personalInfo.setLastName(personalInfoDto.getLastName());
        personalInfo.setEmail(personalInfoDto.getEmail());
        personalInfo.setPhoneNumber(personalInfoDto.getPhoneNumber());
        personalInfo.setDateOfBirth(parseDate(personalInfoDto.getDateOfBirth()));
        personalInfo.setAddress(address);
        personalInfoRepo.save(personalInfo);
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", dateString);
            throw new LoanApplicationException("Invalid date format. Use YYYY-MM-DD");
        }
    }

    private void saveFinancialInformation(LoanApplicationRequest request, LoanApplication application) {
        FinancialInfo financialInfo = createFinancialInfo(request.getFinancialInfo(), application);
        saveEmploymentHistory(request.getFinancialInfo(), financialInfo);  // New line
        saveDebtsAndAssets(request.getFinancialInfo(), financialInfo);
    }

    private void saveEmploymentHistory(LoanApplicationRequest.FinancialInfo financialInfoDto,
                                       FinancialInfo financialInfo) {
        List<EmploymentHistory> employmentHistory = financialInfoDto.getEmployment().stream()
                .map(empDto -> {
                    EmploymentHistory history = new EmploymentHistory();
                    history.setFinancialInfo(financialInfo);
                    history.setEmployerName(empDto.getEmployerName());
                    history.setPosition(empDto.getPosition());
                    history.setEmploymentType(empDto.getEmploymentType());
                    history.setStartDate(empDto.getStartDate());
                    history.setEndDate(empDto.getEndDate());
                    history.setDurationMonths(empDto.getDurationMonths());
                    return history;
                })
                .collect(Collectors.toList());

        employmentHistoryRepo.saveAll(employmentHistory);
    }

    private FinancialInfo createFinancialInfo(LoanApplicationRequest.FinancialInfo financialInfoDto,
                                              LoanApplication application) {
        FinancialInfo financialInfo = new FinancialInfo();
        financialInfo.setLoanApplication(application);

        // Remove these lines:
        // financialInfo.setEmployerName(financialInfoDto.getEmployment().getEmployerName());
        // financialInfo.setPosition(financialInfoDto.getEmployment().getPosition());
        // financialInfo.setEmploymentType(financialInfoDto.getEmployment().getEmploymentType());
        // financialInfo.setEmploymentDuration(financialInfoDto.getEmployment().getEmploymentDuration());

        financialInfo.setMonthlyIncome(financialInfoDto.getMonthlyIncome());
        financialInfo.setCreditScore(financialInfoDto.getCreditScore());
        financialInfo.setMonthlyExpenses(financialInfoDto.getMonthlyExpenses());

        return financialInfoRepo.save(financialInfo);
    }

    private void saveDebtsAndAssets(LoanApplicationRequest.FinancialInfo financialInfoDto,
                                    FinancialInfo financialInfo) {
        saveDebts(financialInfoDto, financialInfo);
        saveAssets(financialInfoDto, financialInfo);
    }

    private void saveDebts(LoanApplicationRequest.FinancialInfo financialInfoDto, FinancialInfo financialInfo) {
        List<Debt> debts = financialInfoDto.getDebts().stream()
                .map(debtDto -> createDebtEntity(debtDto, financialInfo))
                .collect(Collectors.toList());
        debtRepo.saveAll(debts);
    }

    private Debt createDebtEntity(LoanApplicationRequest.Debt debtDto, FinancialInfo financialInfo) {
        Debt debt = new Debt();
        debt.setFinancialInfo(financialInfo);
        debt.setDebtType(debtDto.getDebtType());
        debt.setLender(debtDto.getLender());
        debt.setOutstandingAmount(debtDto.getOutstandingAmount());
        debt.setMonthlyPayment(debtDto.getMonthlyPayment());
        debt.setInterestRate(debtDto.getInterestRate());
        debt.setRemainingTerm(debtDto.getRemainingTerm());
        return debt;
    }

    private void saveAssets(LoanApplicationRequest.FinancialInfo financialInfoDto, FinancialInfo financialInfo) {
        List<Asset> assets = financialInfoDto.getAssets().stream()
                .map(assetDto -> createAssetEntity(assetDto, financialInfo))
                .collect(Collectors.toList());
        assetRepo.saveAll(assets);
    }

    private Asset createAssetEntity(LoanApplicationRequest.Asset assetDto, FinancialInfo financialInfo) {
        Asset asset = new Asset();
        asset.setFinancialInfo(financialInfo);
        asset.setAssetType(assetDto.getAssetType());
        asset.setDescription(assetDto.getDescription());
        asset.setEstimatedValue(assetDto.getEstimatedValue());
        return asset;
    }

    private void saveDocuments(LoanApplication application, List<LoanApplicationRequest.DocumentRequest> documents) {
        documents.forEach(docDto -> {
            validateDocument(docDto);
            Document document = new Document();
            document.setLoanApplication(application);
            document.setDocumentType(docDto.getDocumentType());
            document.setFileData(docDto.getFile());
            documentRepo.save(document);
        });
    }

    private void validateDocument(LoanApplicationRequest.DocumentRequest docDto) {
        if (docDto.getFile().length() > 5_000_000) {
            throw new LoanApplicationException("Document size exceeds 5MB limit");
        }
        if (!List.of("PDF", "JPG", "PNG").contains(docDto.getDocumentType().toUpperCase())) {
            throw new LoanApplicationException("Invalid document type. Allowed: PDF, JPG, PNG");
        }
    }

    private void saveFinancialSummary(User user, LoanApplicationRequest request) {
        FinancialSummary summary = new FinancialSummary();
        summary.setUser(user);
        summary.setCreditScore(request.getFinancialInfo().getCreditScore()); // Use actual credit score
        summary.setScoreRange(calculateScoreRange(request.getFinancialInfo().getCreditScore())); // Add a method to calculate score range
        summary.setMonthlyIncome(request.getFinancialInfo().getMonthlyIncome()); // Use actual monthly income
        summary.setMonthlyExpenses(request.getFinancialInfo().getMonthlyExpenses()); // Use actual monthly expenses
        summary.setLastUpdated(LocalDateTime.now());
        financialSummaryRepo.save(summary);
    }

    private String calculateScoreRange(Integer creditScore) {
        if (creditScore >= 750 && creditScore <= 850) return "Excellent (750-850)";
        if (creditScore >= 700 && creditScore < 750) return "Good (700-749)";
        if (creditScore >= 650 && creditScore < 700) return "Fair (650-699)";
        if (creditScore >= 600 && creditScore < 650) return "Poor (600-649)";
        return "Very Poor (Below 600)";
    }

    private void saveApplicationResult(LoanApplication application, LoanResultDTO result) {
        LoanApplicationResult appResult = new LoanApplicationResult();
        appResult.setLoanApplication(application);
        appResult.setStatus("APPROVED");
        appResult.setMessage("Dummy approval for testing");
        appResult.setEligibilityScore(result.eligibilityScore());
        appResult.setMaxEligibleAmount(result.maxEligibleAmount());
        appResult.setSuggestedInterestRate(result.suggestedInterestRate());
        appResult.setSuggestedTerm(result.suggestedTerm());
        appResult.setEstimatedMonthlyPayment(result.estimatedMonthlyPayment());
        loanApplicationResultRepo.save(appResult);
    }
}