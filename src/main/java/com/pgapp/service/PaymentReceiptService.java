package com.pgapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PaymentReceiptService {

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${MAIL_FROM:no-reply@nivora.work}")
    private String fromEmail;

    @Value("${app.testing:false}")
    private boolean testing;

    // =====================================================
    // 🧾 PDF GENERATION (UNCHANGED & SAFE)
    // =====================================================
    public byte[] generateReceiptPdf(
            String candidateName,
            String roomNo,
            String pgName,
            String pgAddress,
            String pgMobile,
            String pgEmail,
            String ownerName,
            String paymentMonth,
            String paymentYear,
            String paymentMethod,
            String paymentStatus,
            String paymentDate,
            String advanceAmount,
            String amountPaid,
            String balanceAmount,
            String transactionId,
            String receiptId) {

        Context context = new Context();

        context.setVariable("candidateName", candidateName);
        context.setVariable("roomNo", roomNo);
        context.setVariable("paymentMonth", paymentMonth);
        context.setVariable("paymentYear", paymentYear);
        context.setVariable("paymentMethod", paymentMethod);
        context.setVariable("paymentStatus", paymentStatus);
        context.setVariable("paymentDate", paymentDate);
        context.setVariable("advance", advanceAmount);
        context.setVariable("amountPaid", amountPaid);
        context.setVariable("balance", balanceAmount);
        context.setVariable("txnId", transactionId);
        context.setVariable("receiptId", receiptId);

        context.setVariable("generatedOn",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

        context.setVariable("pgName", pgName);
        context.setVariable("pgAddress", pgAddress);
        context.setVariable("pgMobile", pgMobile);
        context.setVariable("pgEmail", pgEmail);

        context.setVariable("ownerName", ownerName);
        context.setVariable("ownerEmail", pgEmail);
        context.setVariable("ownerMobile", pgMobile);

        String htmlContent = templateEngine.process("payment-receipt", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(htmlContent, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    // =====================================================
    // 📧 SEND RECEIPT EMAIL (SENDGRID – FIXED)
    // =====================================================
    public void sendReceiptEmail(
            String candidateEmail,
            String ownerEmail,
            String subject,
            String htmlBody,
            byte[] pdfBytes,
            String filename) {

        if (testing) {
            System.out.println("🧪 TEST MODE – Receipt email not sent");
            return;
        }

        try {
            Email from = new Email(fromEmail);
            Email to = new Email(candidateEmail);

            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(subject);

            Personalization personalization = new Personalization();
            personalization.addTo(to);

            if (ownerEmail != null && !ownerEmail.isBlank()) {
                personalization.addCc(new Email(ownerEmail));
            }

            mail.addPersonalization(personalization);

            mail.addContent(new Content("text/html", htmlBody));

            Attachments attachment = new Attachments();
            attachment.setContent(Base64.getEncoder().encodeToString(pdfBytes));
            attachment.setType("application/pdf");
            attachment.setFilename(filename);
            attachment.setDisposition("attachment");

            mail.addAttachments(attachment);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            sg.api(request);

            System.out.println("✅ Receipt email sent to " + candidateEmail);

        } catch (Exception e) {
            System.err.println("❌ Receipt email failed");
            e.printStackTrace();
        }
    }
}
