package com.team1_5.credwise.service;

import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final EmailService emailService;
    private final SMSService smsService;

    public NotificationService(EmailService emailService, SMSService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void sendLoanApplicationStatusUpdate(User user, LoanApplication.ApplicationStatus status) {
        // Send email notification
        emailService.sendEmail(
                user.getEmail(),
                "Loan Application Status Update",
                "Your loan application status has been updated to: " + status
        );

        // Send SMS notification
        smsService.sendSMS(
                user.getPhoneNumber(),
                "Your loan application status has been updated to: " + status
        );
    }

    public void sendPasswordResetNotification(User user, String resetToken) {
        emailService.sendPasswordResetLink(user.getEmail(), resetToken);
    }
}