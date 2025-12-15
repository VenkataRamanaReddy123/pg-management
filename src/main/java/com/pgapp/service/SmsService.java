package com.pgapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private final Logger log = LoggerFactory.getLogger(SmsService.class);

    public void sendSms(String mobile, String message) {
        // Simulation: log the message. Replace with Twilio/other provider in production.
        log.info("SMS -> {} : {}", mobile, message);
    }

    // New method for forgot password
    public void sendResetToken(String mobile, String token) {
        String message = "Your PGApp password reset token is: " + token;
        sendSms(mobile, message);  // reuse existing method
    }
}
