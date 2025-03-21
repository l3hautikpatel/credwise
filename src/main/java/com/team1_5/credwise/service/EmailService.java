package com.team1_5.credwise.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private final JavaMailSender emailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender emailSender, SpringTemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    public void sendRegistrationConfirmation(String to, String username) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariable("username", username);
            String html = templateEngine.process("registration-confirmation", context);

            helper.setTo(to);
            helper.setFrom("noreply@credwise.com");
            helper.setSubject("Welcome to Credwise - Registration Confirmation");
            helper.setText(html, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            // Handle email sending errors
            throw new RuntimeException("Error sending registration email", e);
        }
    }

    public void sendPasswordResetLink(String to, String resetToken) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariable("resetLink", "https://credwise.com/reset-password?token=" + resetToken);
            String html = templateEngine.process("password-reset", context);

            helper.setTo(to);
            helper.setFrom("noreply@credwise.com");
            helper.setSubject("Password Reset Request");
            helper.setText(html, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending password reset email", e);
        }
    }
}