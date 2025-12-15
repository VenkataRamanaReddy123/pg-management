package com.pgapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends OTP to given email
     */
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(purpose + " OTP for PG App");
            message.setText("Your OTP for " + purpose + " is: " + otp);

            mailSender.send(message);
            System.out.println("✅ OTP email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Error sending OTP email to: " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * ✅ Sends registration email (supports masked password & MPIN)
     * If password or mpin is null, they are not included in the email
     */
    public void sendRegistrationEmail(
            String toEmail,
            String ownerName,
            String pgName,
            String pgAddress,
            String mobile,
            String password,
            String mpin) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("PG Registration Successful");

            String maskedPassword = maskPassword(password);
            String maskedMpin = maskMpin(mpin);

            String text = "Hello " + ownerName + ",\n\n"
                    + "You have successfully registered your PG. Here are the details you entered:\n\n"
                    + "PG Name: " + pgName + "\n"
                    + "PG Address: " + pgAddress + "\n"
                    + "Mobile: " + mobile + "\n"
                    + "Email: " + toEmail + "\n";

            // Add masked password & MPIN if provided
            if (password != null) {
                text += "Password: " + maskedPassword + "\n";
            }
            if (mpin != null) {
                text += "MPIN: " + maskedMpin + "\n";
            }

            text += "\nThanks,\nYour PG App Team";

            message.setText(text);
            mailSender.send(message);

            System.out.println("✅ Registration email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send registration email to: " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * ✅ New method: Send notification email when Password or MPIN changes
     * Shows masked values in the email for security
     */
    public void sendCredentialChangeEmail(
            String toEmail,
            String ownerName,
            String password,   // Pass null if only MPIN changed
            String mpin        // Pass null if only Password changed
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Credentials Changed Successfully");

            String text = "Hello " + ownerName + ",\n\n"
                    + "Your credentials have been updated successfully.\n";

            // Include masked password if changed
            if (password != null) {
                text += "New Password: " + maskPassword(password) + "\n";
            }

            // Include masked MPIN if changed
            if (mpin != null) {
                text += "New MPIN: " + maskMpin(mpin) + "\n";
            }

            text += "\nIf you did not make this change, please Change Password/Mpin immediately.\n"
                    + "\nThanks,\nYour PG App Team";

            message.setText(text);
            mailSender.send(message);

            System.out.println("✅ Credential change email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send credential change email to: " + toEmail);
            e.printStackTrace();
        }
    }

    // ========================= HELPER METHODS =========================

    /**
     * Mask password for security in emails
     * Example: Password123 -> Pa******23
     */
    private String maskPassword(String password) {
        if (password == null || password.length() < 4) {
            return "********";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }

    /**
     * Mask MPIN for security in emails
     * Example: 1234 -> 1**4
     */
    private String maskMpin(String mpin) {
        if (mpin == null || mpin.length() != 4) {
            return "****";
        }
        return mpin.charAt(0) + "**" + mpin.charAt(3);
    }
}
