package com.pgapp.model;

/**
 * Enum representing the status of a payment.
 * These values are stored in the PaymentHistory table as strings.
 */
public enum PaymentStatus {
    PENDING,       // Payment has not been made yet
    PARTIAL_PAID,  // Partial payment has been made
    PAID           // Full payment has been completed
}
