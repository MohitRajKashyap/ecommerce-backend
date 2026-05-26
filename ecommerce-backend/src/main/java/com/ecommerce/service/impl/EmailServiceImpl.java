package com.ecommerce.service.impl;

import com.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email notification service — all methods are @Async to avoid blocking the main thread.
 * Production upgrade: use Thymeleaf HTML templates for rich emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.name}")
    private String appName;

    @Async("taskExecutor")
    @Override
    public void sendWelcomeEmail(String to, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Welcome to " + appName + "!");
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "Welcome to " + appName + "! Your account has been created successfully.\n\n" +
                "Start shopping at: " + frontendUrl + "\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
            log.debug("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void sendPasswordResetEmail(String to, String firstName, String resetToken) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Reset Your " + appName + " Password");
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "You requested a password reset. Click the link below (expires in 15 minutes):\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
            log.debug("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void sendOrderConfirmationEmail(String to, String firstName,
                                           String orderNumber, String totalAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Order Confirmed: " + orderNumber);
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "Your order " + orderNumber + " has been confirmed!\n" +
                "Total Amount: ₹" + totalAmount + "\n\n" +
                "Track your order at: " + frontendUrl + "/orders/" + orderNumber + "\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void sendOrderStatusUpdateEmail(String to, String firstName,
                                           String orderNumber, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Order Update: " + orderNumber + " is " + status);
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "Your order " + orderNumber + " status has been updated to: " + status + "\n\n" +
                "Track your order at: " + frontendUrl + "/orders/" + orderNumber + "\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send order status email: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void sendPaymentSuccessEmail(String to, String firstName,
                                        String orderNumber, String amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Payment Successful for Order " + orderNumber);
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "Payment of ₹" + amount + " for order " + orderNumber + " was successful!\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send payment success email: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void sendPaymentFailureEmail(String to, String firstName, String orderNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Payment Failed for Order " + orderNumber);
            message.setText(
                "Hi " + firstName + ",\n\n" +
                "Payment for order " + orderNumber + " has failed. Please retry.\n\n" +
                "Retry at: " + frontendUrl + "/orders/" + orderNumber + "\n\n" +
                "Best regards,\nThe " + appName + " Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send payment failure email: {}", e.getMessage());
        }
    }
}
