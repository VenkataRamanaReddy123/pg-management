package com.pgapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentReceiptService {

    @Autowired
    private TemplateEngine templateEngine; // Thymeleaf template engine

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;
    @Autowired
    private JavaMailSender mailSender; // For sending emails

    /**
     * 🔹 Generate PDF from Thymeleaf template
     * 
     * @param candidateName  Name of the candidate
     * @param roomNo         Room number of candidate
     * @param pgName         PG name
     * @param pgAddress      PG address
     * @param pgMobile       PG contact number
     * @param pgEmail        PG email
     * @param ownerName      PG owner name
     * @param paymentMonth   Payment month
     * @param paymentYear    Payment year
     * @param paymentMethod  Payment method (CASH/ONLINE)
     * @param paymentStatus  Payment status (PAID/PENDING)
     * @param paymentDate    Payment date
     * @param advanceAmount  Advance amount paid
     * @param amountPaid     Amount paid
     * @param balanceAmount  Remaining balance
     * @param transactionId  Transaction ID
     * @param receiptId      Generated receipt ID
     * @return byte[]       PDF bytes
     */
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

        // -----------------------------
        // Candidate & Payment Variables
        // -----------------------------
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

        // Timestamp for when PDF is generated
        context.setVariable("generatedOn",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

        // -----------------------------
        // PG Details
        // -----------------------------
        context.setVariable("pgName", pgName);
        context.setVariable("pgAddress", pgAddress);
        context.setVariable("pgMobile", pgMobile);
        context.setVariable("pgEmail", pgEmail);

        // -----------------------------
        // Owner Details
        // -----------------------------
        context.setVariable("ownerName", ownerName);
        context.setVariable("ownerEmail", pgEmail);   // mapped to PG email
        context.setVariable("ownerMobile", pgMobile); // mapped to PG mobile

        // Render HTML using Thymeleaf template
        String htmlContent = templateEngine.process("payment-receipt", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Convert HTML to PDF using iText
            HtmlConverter.convertToPdf(htmlContent, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    /**
     * 🔹 Send Payment Receipt Email with PDF attachment
     *
     * @param candidateEmail Candidate's email address
     * @param pgOwnerEmail   PG owner's email address (optional CC)
     * @param subject        Email subject
     * @param bodyHtml       Email body in HTML
     * @param pdfBytes       PDF content as byte array
     * @param filename       PDF filename
     * @throws MessagingException if email sending fails
     */
    public void sendReceiptEmail(String candidateEmail, String pgOwnerEmail, String subject, String bodyHtml,
            byte[] pdfBytes, String filename) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("pgapp@gmail.com");       // Sender email
        helper.setTo(candidateEmail);            // Primary recipient (candidate)
        // helper.setCc(pgOwnerEmail);           // Optional CC to PG owner
        helper.setSubject(subject);              // Email subject
        helper.setText(bodyHtml, true);          // HTML content
        helper.addAttachment(filename, new ByteArrayResource(pdfBytes)); // Attach PDF

        // Send email
        mailSender.send(message);

        System.out.println("Email sent to: " + candidateEmail + " | CC: " + pgOwnerEmail + " | Receipt ID: " + filename);
    }

}
