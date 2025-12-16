package com.pgapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${MAIL_FROM:no-reply@nivora.work}")
    private String fromEmail;

    @Value("${app.testing:false}")
    private boolean testing;

    // =====================================================
    // COMMON SEND METHOD
    // =====================================================
    private void sendEmail(String to, String subject, String body) {

        // 🟡 TEST MODE → do not send real email
        if (testing) {
            System.out.println("🧪 TEST MODE EMAIL");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println(body);
            return;
        }

        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);

            System.out.println("✅ Email sent to: " + to);

        } catch (IOException e) {
            // ❌ Never crash the app
            System.err.println("❌ Email failed to: " + to);
            e.printStackTrace();
        }
    }

    // =====================================================
    // OTP EMAIL
    // =====================================================
    public void sendOtpEmail(String toEmail, String otp, String purpose) {

        String subject = purpose + " OTP – PG App";
        String body =
                "Hello,\n\n"
              + "Your OTP for " + purpose + " is:\n\n"
              + otp + "\n\n"
              + "Do not share this OTP with anyone.\n\n"
              + "Thanks,\nPG App Team";

        sendEmail(toEmail, subject, body);
    }

    // =====================================================
    // REGISTRATION EMAIL
    // =====================================================
    public void sendRegistrationEmail(
            String toEmail,
            String ownerName,
            String pgName,
            String pgAddress,
            String mobile,
            String password,
            String mpin) {

        String subject = "PG Registration Successful";

        String body =
                "Hello " + ownerName + ",\n\n"
              + "Your PG has been registered successfully.\n\n"
              + "PG Name: " + pgName + "\n"
              + "PG Address: " + pgAddress + "\n"
              + "Mobile: " + mobile + "\n"
              + "Email: " + toEmail + "\n";

        if (password != null) {
            body += "Password: " + maskPassword(password) + "\n";
        }
        if (mpin != null) {
            body += "MPIN: " + maskMpin(mpin) + "\n";
        }

        body += "\nThanks,\nPG App Team";

        sendEmail(toEmail, subject, body);
    }

    // =====================================================
    // PASSWORD / MPIN CHANGE EMAIL
    // =====================================================
    public void sendCredentialChangeEmail(
            String toEmail,
            String ownerName,
            String password,
            String mpin) {

        String subject = "Credentials Updated – PG App";

        String body =
                "Hello " + ownerName + ",\n\n"
              + "Your credentials were updated successfully.\n\n";

        if (password != null) {
            body += "New Password: " + maskPassword(password) + "\n";
        }
        if (mpin != null) {
            body += "New MPIN: " + maskMpin(mpin) + "\n";
        }

        body +=
                "\nIf this was not you, please reset your credentials immediately.\n\n"
              + "Thanks,\nPG App Team";

        sendEmail(toEmail, subject, body);
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private String maskPassword(String password) {
        if (password == null || password.length() < 4) {
            return "********";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }

    private String maskMpin(String mpin) {
        if (mpin == null || mpin.length() != 4) {
            return "****";
        }
        return mpin.charAt(0) + "**" + mpin.charAt(3);
    }
}
