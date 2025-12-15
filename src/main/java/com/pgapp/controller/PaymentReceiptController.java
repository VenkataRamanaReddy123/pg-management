package com.pgapp.controller;

import com.pgapp.model.Candidate;
import com.pgapp.model.Pg;
import com.pgapp.repository.CandidateRepository;
import com.pgapp.repository.PaymentHistoryRepository;
import com.pgapp.repository.PgRepository;
import com.pgapp.service.PaymentReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
public class PaymentReceiptController {

    @Autowired
    private PaymentReceiptService receiptService;

    @Autowired
    private CandidateRepository candidateRepo;

    @Autowired
    private PgRepository pgRepo;

    @Autowired
    private PaymentHistoryRepository paymentRepo;

    // ------------------------------------------------------------------
    // 🧾 SEND PAYMENT RECEIPT (PDF + Email)
    // Handles POST request to "/payments/send-receipt".
    // Generates a PDF receipt and sends it via email to the candidate and PG owner.
    // Preserves roomNo filter in redirect for UI consistency.
    // ------------------------------------------------------------------
    @PostMapping("/payments/send-receipt")
    public String sendPaymentReceipt(
            @RequestParam Long candidateId,
            @RequestParam Long pgId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam String paymentMethod,
            @RequestParam String paymentStatus,
            @RequestParam String paymentDate,
            @RequestParam String roomNo, // <-- Keep roomNo to preserve filter in redirect
            @RequestParam(required = false) String txnId,
            @RequestParam(required = false) String receiptId,
            RedirectAttributes redirectAttrs) {

        // 🔹 Fetch candidate and PG from database
        Optional<Candidate> candidateOpt = candidateRepo.findById(candidateId);
        Optional<Pg> pgOpt = pgRepo.findById(pgId);

        // 🔹 If either candidate or PG not found, redirect back with failure flag
        if (candidateOpt.isEmpty() || pgOpt.isEmpty()) {
            redirectAttrs.addAttribute("receiptSent", "false");
            return "redirect:/payments/history?pgId=" + pgId + "&month=" + month + "&year=" + year + "&roomNo=" + roomNo;
        }

        Candidate candidate = candidateOpt.get();
        Pg pg = pgOpt.get();

        // 🔹 Check if candidate joined after the selected month → cannot send receipt
        LocalDate joinDate = candidate.getJoiningDate() != null
                ? candidate.getJoiningDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        if (joinDate != null && joinDate.isAfter(firstOfMonth)) {
            redirectAttrs.addAttribute("receiptSent", "false");
            return "redirect:/payments/history?pgId=" + pgId + "&month=" + month + "&year=" + year + "&roomNo=" + roomNo;
        }

        // 🔹 Fetch payment amounts from DB (amountPaid, advance, balance)
        Double amountPaid = paymentRepo.findAmount(candidateId, month, year);
        Double advanceAmount = paymentRepo.findAdvance(candidateId, month, year);
        Double balanceAmount = paymentRepo.findBalance(candidateId, month, year);

        // 🔹 Default null amounts to 0.0
        amountPaid = amountPaid != null ? amountPaid : 0.0;
        advanceAmount = advanceAmount != null ? advanceAmount : 0.0;
        balanceAmount = balanceAmount != null ? balanceAmount : 0.0;

        // 🔹 Use existing transaction ID if available, else keep provided or generate new
        if (txnId == null || txnId.isEmpty()) {
            txnId = paymentRepo.findTransactionId(candidateId, month, year);
        }
        // 🔹 Generate receiptId if not provided
        if (receiptId == null || receiptId.isEmpty()) {
            receiptId = "RCPT-" + System.currentTimeMillis();
        }

        // 🔹 Format payment date for PDF display (dd-MMM-yyyy)
        String formattedPaymentDate = paymentDate;
        try {
            LocalDate date = LocalDate.parse(paymentDate);
            formattedPaymentDate = date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        } catch (Exception ignored) {}

        // 🔹 Generate PDF receipt bytes using receiptService
        byte[] pdfBytes = receiptService.generateReceiptPdf(
                candidate.getName(),
                candidate.getRoomNo(),
                pg.getPgName(),
                pg.getAddress(),
                pg.getMobile(),
                pg.getEmail(),
                pg.getOwner().getOwnerName(),
                String.valueOf(month),
                String.valueOf(year),
                paymentMethod,
                paymentStatus,
                formattedPaymentDate,
                String.valueOf(advanceAmount),
                String.valueOf(amountPaid),
                String.valueOf(balanceAmount),
                txnId,
                receiptId
        );

        // 🔹 Construct PDF filename
        String pdfFilename = candidate.getName() + "-" + month + "-" + year + ".pdf";

        // 🔹 Send receipt email to candidate and PG owner
        try {
            receiptService.sendReceiptEmail(
                    candidate.getEmail(),
                    pg.getOwner().getEmail(),
                    "Payment Receipt - " + month + "/" + year,
                    "<p>Dear " + candidate.getName() + ",<br/>Please find your payment receipt attached.</p>",
                    pdfBytes,
                    pdfFilename
            );
            redirectAttrs.addAttribute("receiptSent", "true"); // ✅ Success
        } catch (MessagingException e) {
            e.printStackTrace();
            redirectAttrs.addAttribute("receiptSent", "false"); // ❌ Failure
        }

        // 🔹 Redirect back to payment history page, preserving filters including roomNo
        return "redirect:/payments/history?pgId=" + pgId + "&month=" + month + "&year=" + year + "&roomNo=" + roomNo;
    }

}
