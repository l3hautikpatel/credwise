package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.model.*;
import com.team1_5.credwise.repository.*;
import com.team1_5.credwise.util.CreditScoreValidator;
import com.team1_5.credwise.util.LoanResultCalculator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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


    public LoanApplicationService(LoanApplicationRepository loanAppRepo,
                                  AddressRepository addressRepo,
                                  FinancialInfoRepository financialInfoRepo,
                                  DebtRepository debtRepo,
                                  AssetRepository assetRepo,
                                  DocumentRepository documentRepo,
                                  CreditScoreValidator creditValidator,
                                  LoanResultCalculator resultCalculator,
                                  UserRepository userRepo, PersonalInfoRepository personalInfoRepo,
                                  LoanApplicationResultRepository loanApplicationResultRepo) {
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
    }

    @Transactional
    public LoanApplicationResponse processLoanApplication(Long userId, LoanApplicationRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Save Loan Application
        LoanApplication application = saveLoanApplication(user, request);

        // 2. Validate Credit Score
        boolean isCreditValid = creditValidator.validate(request);

        // 3. Calculate Result
        boolean isApproved = resultCalculator.calculateResult(request, isCreditValid);

        // 4. Save Final Result
        saveApplicationResult(application, isApproved);

        return new LoanApplicationResponse(
                application.getId().toString(),
                "Loan application submitted successfully",
                isApproved ? "APPROVED" : "UNDER_REVIEW"
        );
    }


//    private LoanApplication saveLoanApplication(User user, LoanApplicationRequest request) {
//        // Save Address
//        Address address = saveAddress(request.getPersonalInfo().getAddress());
//
//        // Save Personal Info
//        PersonalInfo personalInfo = savePersonalInfo(request.getPersonalInfo(), address);
//
//        // Save Financial Info with Debts and Assets
//        FinancialInfo financialInfo = saveFinancialInfo(request.getFinancialInfo());
//
//        // Save Loan Application
//        LoanApplication application = new LoanApplication();
//        application.setUser(user);
//        application.setProductType(request.getLoanDetails().getProductType());
//        application.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
//        application.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
//        application.setRequestedTerm(request.getLoanDetails().getRequestedTerm());
//        application.setStatus("PENDING");
//        LoanApplication savedApplication = loanAppRepo.save(application);
//
//        // Link entities
//        personalInfo.setLoanApplication(savedApplication);
//        financialInfo.setLoanApplication(savedApplication);
//
//        // Save Documents
//        saveDocuments(savedApplication, request.getDocuments());
//
//        return savedApplication;
//    }



    private LoanApplication saveLoanApplication(User user, LoanApplicationRequest request) {
        // 1. FIRST: Save Loan Application (parent entity)
        LoanApplication application = new LoanApplication();
        application.setUser(user);
        application.setProductType(request.getLoanDetails().getProductType());
        application.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
        application.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
        application.setRequestedTerm(request.getLoanDetails().getRequestedTerm());
        application.setStatus("PENDING");
        LoanApplication savedApplication = loanAppRepo.save(application);

        // 2. Save Address
        Address address = saveAddress(request.getPersonalInfo().getAddress());

        // 3. Save PersonalInfo WITH loan application reference
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setLoanApplication(savedApplication); // Critical fix
        personalInfo.setFirstName(request.getPersonalInfo().getFirstName());
        personalInfo.setLastName(request.getPersonalInfo().getLastName());
        personalInfo.setEmail(request.getPersonalInfo().getEmail());
        personalInfo.setPhoneNumber(request.getPersonalInfo().getPhoneNumber());
        personalInfo.setDateOfBirth(LocalDate.parse(request.getPersonalInfo().getDateOfBirth()));
        personalInfo.setAddress(address);
        personalInfoRepo.save(personalInfo);

        // 4. Save FinancialInfo WITH loan application reference
        FinancialInfo financialInfo = saveFinancialInfo(request.getFinancialInfo(), savedApplication);

        // 5. Save Documents
        saveDocuments(savedApplication, request.getDocuments());

        return savedApplication;
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

    private PersonalInfo savePersonalInfo(LoanApplicationRequest.PersonalInfo personalInfoDto, Address address) {
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setFirstName(personalInfoDto.getFirstName());
        personalInfo.setLastName(personalInfoDto.getLastName());
        personalInfo.setEmail(personalInfoDto.getEmail());
        personalInfo.setPhoneNumber(personalInfoDto.getPhoneNumber());
        personalInfo.setDateOfBirth(LocalDate.parse(personalInfoDto.getDateOfBirth()));
        personalInfo.setAddress(address);
        return personalInfoRepo.save(personalInfo);
    }

    private FinancialInfo saveFinancialInfo(LoanApplicationRequest.FinancialInfo financialInfoDto ,LoanApplication application ) {
        FinancialInfo financialInfo = new FinancialInfo();
        financialInfo.setLoanApplication(application);
        // Map all financial info fields
        financialInfo.setEmployerName(financialInfoDto.getEmployment().getEmployerName());
        financialInfo.setPosition(financialInfoDto.getEmployment().getPosition());
        financialInfo.setEmploymentType(financialInfoDto.getEmployment().getEmploymentType());
        financialInfo.setEmploymentDuration(financialInfoDto.getEmployment().getEmploymentDuration());
        financialInfo.setMonthlyIncome(financialInfoDto.getMonthlyIncome());
        financialInfo.setCreditScore(financialInfoDto.getCreditScore());
        financialInfo.setMonthlyExpenses(financialInfoDto.getMonthlyExpenses());

        FinancialInfo savedInfo = financialInfoRepo.save(financialInfo);

        // Save Debts
//        List<Debt> debts = financialInfoDto.getDebts().stream()
//                .map(debtDto -> {
//                    Debt debt = new Debt();
//                    debt.setFinancialInfo(savedInfo);
//                    // Map debt fields
//                    return debtRepo.save(debt);
//                }).collect(Collectors.toList());
//
//        // Save Assets
//        List<Asset> assets = financialInfoDto.getAssets().stream()
//                .map(assetDto -> {
//                    Asset asset = new Asset();
//                    asset.setFinancialInfo(savedInfo);
//                    // Map asset fields
//                    return assetRepo.save(asset);
//                }).collect(Collectors.toList());
//
//        savedInfo.setDebts(debts);
//        savedInfo.setAssets(assets);


        // Save debts and assets
        saveDebtsAndAssets(financialInfoDto, savedInfo);

        return savedInfo;
    }


    private void saveDebtsAndAssets(LoanApplicationRequest.FinancialInfo financialInfoDto,
                                    FinancialInfo financialInfo) {
        List<Debt> debts = financialInfoDto.getDebts().stream()
                .map(debtDto -> {
                    Debt debt = new Debt();
                    debt.setDebtType(debtDto.getDebtType()); // CRITICAL LINE
                    debt.setLender(debtDto.getLender());
                    debt.setOutstandingAmount(debtDto.getOutstandingAmount());
                    debt.setMonthlyPayment(debtDto.getMonthlyPayment());
                    debt.setInterestRate(debtDto.getInterestRate());
                    debt.setRemainingTerm(debtDto.getRemainingTerm());
                    debt.setFinancialInfo(financialInfo);
                    return debt;
                }).collect(Collectors.toList());

        debtRepo.saveAll(debts);  // Save debts after mapping
    }


    private void saveDocuments(LoanApplication application, List<LoanApplicationRequest.DocumentRequest> documents) {
        documents.forEach(docDto -> {
            Document document = new Document();
            document.setLoanApplication(application);
            document.setDocumentType(docDto.getDocumentType());
            document.setFileData(docDto.getFile());
            documentRepo.save(document);
        });
    }

    private void saveApplicationResult(LoanApplication application, boolean isApproved) {
        LoanApplicationResult result = new LoanApplicationResult();
        result.setLoanApplication(application);
        result.setStatus(isApproved ? "APPROVED" : "UNDER_REVIEW");
        result.setMessage(isApproved ? "Application approved" : "Pending verification");
        // Set other result fields
        loanApplicationResultRepo.save(result);
    }
}