package com.ecommerce.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String firstName);
    void sendPasswordResetEmail(String to, String firstName, String resetToken);
    void sendOrderConfirmationEmail(String to, String firstName, String orderNumber, String totalAmount);
    void sendOrderStatusUpdateEmail(String to, String firstName, String orderNumber, String status);
    void sendPaymentSuccessEmail(String to, String firstName, String orderNumber, String amount);
    void sendPaymentFailureEmail(String to, String firstName, String orderNumber);
}
