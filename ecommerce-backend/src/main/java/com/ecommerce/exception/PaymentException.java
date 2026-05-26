package com.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when payment processing fails. */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
