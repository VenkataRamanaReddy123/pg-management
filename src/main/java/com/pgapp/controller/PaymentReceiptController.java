package com.pgapp.controller;

import com.pgapp.model.*;
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

    // ==========================================================
    // 🧾 SEND PAYMENT RECEIPT (DB-DRIVEN, SAFE, FINAL)
    // ==========================================================
    @PostMapping("/payments/send-receipt")
    public String sendPaymentReceipt(
            @RequestParam Long candidateId,
            @RequestParam Long pgId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String roomNo,
            RedirectAttributes redirectAttrs) {

        try {
            // --------------------------------------------------
            // 1️⃣ Fetch Candidate & PG
            // --------------------------------------------------
            Optional<Candidate> candidateOpt = candidateRepo.findById(candidateId);
            Optional<Pg> pgOpt = pgRepo.findById(pgId);

            if (candidateOpt.isEmpty() || pgOpt.isEmpty()) {
                redirectAttrs.addAttribute("receiptSent", "false");
                return redirect(pgId, month, year, roomNo);
            }

            Candidate candidate = candidateOpt.get();
            Pg pg = pgOpt.get();

            // --------------------------------------------------
            // 2️⃣ Fetch PaymentHistory (SINGLE SOURCE OF TRUTH)
            // --------------------------------------------------
            PaymentHistory payment = paymentRepo
                    .findByCandidate_CandidateIdAndPg_IdAndPaymentMonthAndPaymentYear(
                            candidateId, pgId, month, year)
                    .orElse(null);

            // ❌ No payment or not PAID → stop
            if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
                redirectAttrs.addAttribute("receiptSent", "false");
                return redirect(pgId, month, year, roomNo);
            }
         // ==================================================
         // 🛡️ FIRST-TIME PAID SAFETY FIX (DO NOT REMOVE)
         // ==================================================
         if (payment.getPaymentDate() == null) {
             payment.setPaymentDate(LocalDate.now());
         }

         if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
             payment.setTransactionId(
                 payment.getPaymentMethod() == PaymentMethod.CASH
                     ? "CASH-" + System.currentTimeMillis()
                     : "TXN-" + System.currentTimeMillis()
             );
         }

         if (payment.getReceiptId() == null || payment.getReceiptId().isBlank()) {
             payment.setReceiptId("RCPT-" + System.currentTimeMillis());
         }

         // 🔒 Persist ONCE before email
         paymentRepo.save(payment);
       

            // --------------------------------------------------
            // 4️⃣ Extract values ONLY from DB
            // --------------------------------------------------
            String paymentMethod = payment.getPaymentMethod().name();
            String paymentStatus = payment.getStatus().name();

            Double advance = payment.getAdvance() != null ? payment.getAdvance() : 0.0;
            Double amountPaid = payment.getAmountPaid() != null ? payment.getAmountPaid() : 0.0;
            Double balance = payment.getBalance() != null ? payment.getBalance() : 0.0;

            LocalDate paymentDate = payment.getPaymentDate() != null
                    ? payment.getPaymentDate()
                    : LocalDate.now();

            String formattedPaymentDate =
                    paymentDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));

            // --------------------------------------------------
            // 5️⃣ Transaction & Receipt IDs
            // --------------------------------------------------
            String txnId = payment.getTransactionId();
            if (txnId == null || txnId.isBlank()) {
                txnId = "CASH-" + System.currentTimeMillis();
            }

            String receiptId = payment.getReceiptId();
            if (receiptId == null || receiptId.isBlank()) {
                receiptId = "RCPT-" + System.currentTimeMillis();
                payment.setReceiptId(receiptId); // persist once
                paymentRepo.save(payment);
            }

            // --------------------------------------------------
            // 6️⃣ Generate PDF Receipt
            // --------------------------------------------------
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
                    String.valueOf(advance),
                    String.valueOf(amountPaid),
                    String.valueOf(balance),
                    txnId,
                    receiptId
            );

            String pdfFilename =
                    candidate.getName() + "-" + month + "-" + year + ".pdf";

            // --------------------------------------------------
            // 7️⃣ Send Email (Candidate + Owner)
            // --------------------------------------------------
            receiptService.sendReceiptEmail(
                    candidate.getEmail(),
                    pg.getOwner().getEmail(),
                    "Payment Receipt - " + month + "/" + year,
                    "<p>Dear " + candidate.getName()
                            + ",<br/>Please find your payment receipt attached.</p>",
                    pdfBytes,
                    pdfFilename
            );

            redirectAttrs.addAttribute("receiptSent", "true");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addAttribute("receiptSent", "false");
        } 
        
        // --------------------------------------------------
        // 8️⃣ Redirect back (preserve filters)
        // --------------------------------------------------
        return redirect(pgId, month, year, roomNo);
    }

    // --------------------------------------------------
    // 🔁 Redirect helper
    // --------------------------------------------------
    private String redirect(Long pgId, int month, int year, String roomNo) {
        String url = "redirect:/payments/history?pgId=" + pgId
                + "&month=" + month
                + "&year=" + year;

        if (roomNo != null && !roomNo.isBlank()) {
            url += "&roomNo=" + roomNo;
        }
        return url;
    }
}
