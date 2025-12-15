package com.pgapp.model;

/**
 * Enum representing the available payment methods.
 * These values are stored in the PaymentHistory table as strings.
 */
public enum PaymentMethod {
    PHONEPAY, // Payment via PhonePe
    GPAY,     // Payment via Google Pay
    PAYTM,    // Payment via Paytm
    CRED,     // Payment via CRED app
    CASH      // Cash payment
}
