package com.pgapp.controller;

import com.pgapp.model.*;
import com.pgapp.repository.CandidateRepository;
import com.pgapp.repository.PaymentHistoryRepository;
import com.pgapp.repository.PgRepository;
import com.pgapp.service.PaymentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/payments")
public class PaymentHistoryController {

    @Autowired
    private PaymentHistoryService paymentHistoryService;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private PaymentHistoryRepository paymentRepo;

    @Autowired
    private PgRepository pgRepo;

    @Autowired
    private CandidateRepository candidateRepo;

    // ============================================================
    // 🔷 SHOW PAYMENT HISTORY PAGE
    // Handles GET request to "/payments/history".
    // Displays list of candidates and their payment status for
    // a selected PG, month, and year. Also handles optional roomNo filter.
    // Creates missing payment history entries automatically (default PENDING).
    // ============================================================
    @GetMapping("/history")
    public String viewPaymentHistory(
            @RequestParam(required = false) Long pgId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String roomNo,
            Model model,
            HttpSession session) {

        // 🔹 Check if owner is logged in
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null)
            return "redirect:/login";

        // 🔹 Fetch all PGs for this owner (not deleted)
        List<Pg> ownerPgs = pgRepo.findByOwnerIdAndDeletedFalse(ownerId);
        model.addAttribute("pgs", ownerPgs);

        // 🔹 If no PG selected, default to first PG in list
        if (pgId == null && !ownerPgs.isEmpty()) {
            pgId = ownerPgs.get(0).getId();
        }

        // 🔹 Fetch selected PG details
        Pg selectedPg = pgRepo.findById(pgId).orElse(null);
        model.addAttribute("selectedPg", selectedPg);
        model.addAttribute("pgId", pgId);

        // 🔹 Default to current date for payment inputs
        LocalDate now = LocalDate.now();
        model.addAttribute("defaultDate", now);

        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();

        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);

        // 🔹 Prepare month dropdown (1–12)
        model.addAttribute("months", Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12));

        // 🔹 Keep the roomNo filter in the model
        model.addAttribute("roomNo", roomNo);

        // 🔹 Prepare year dropdown (current year -5 to +10)
        List<Integer> years = new ArrayList<>();
        for (int i = now.getYear() - 5; i <= now.getYear() + 10; i++)
            years.add(i);
        model.addAttribute("years", years);

        // 🔹 Fetch candidates in selected PG
        List<Candidate> candidates;
        if (selectedPg != null) {
            if (roomNo != null && !roomNo.isEmpty()) {
                // Filter by roomNo if provided
                candidates = candidateRepo.findByPgIdOrderByRoomNoAsc(selectedPg.getId()).stream()
                        .filter(c -> c.getRoomNo() != null && c.getRoomNo().contains(roomNo))
                        .toList();
            } else {
                candidates = candidateRepo.findByPgIdOrderByRoomNoAsc(selectedPg.getId());
            }
        } else {
            candidates = Collections.emptyList();
        }

        // 🔹 Determine the last day of the selected month
        LocalDate selectedMonthEnd = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());

        // 🔹 Filter candidates whose joining date is before end of month
        candidates = candidates.stream()
                .filter(c -> {
                    if (c.getJoiningDate() == null) return true;

                    LocalDate joinDate = c.getJoiningDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    return !joinDate.isAfter(selectedMonthEnd);
                })
                .toList();

        // 🔹 Apply roomNo filter AFTER date filter
        if (roomNo != null && !roomNo.isEmpty()) {
            candidates = candidates.stream()
                    .filter(c -> c.getRoomNo() != null && c.getRoomNo().contains(roomNo))
                    .toList();
        }

        // 🔹 Sort candidates naturally by roomNo (1,2,10,101,A1,B2)
        candidates = candidates.stream()
                .sorted((a, b) -> {
                    String r1 = a.getRoomNo() == null ? "" : a.getRoomNo().trim().toUpperCase();
                    String r2 = b.getRoomNo() == null ? "" : b.getRoomNo().trim().toUpperCase();

                    boolean isNum1 = r1.matches("\\d+");
                    boolean isNum2 = r2.matches("\\d+");

                    if (isNum1 && isNum2) {
                        return Integer.compare(Integer.parseInt(r1), Integer.parseInt(r2));
                    }
                    return r1.compareTo(r2);
                })
                .toList();

        // 🔹 Auto-create payment history records if missing (default PENDING)
        final int m = month;
        final int y = year;
        Map<Long, PaymentHistory> paymentMap = new HashMap<>();

        for (Candidate c : candidates) {
            PaymentHistory ph = paymentRepo
                    .findByCandidate_CandidateIdAndPaymentMonthAndPaymentYear(c.getCandidateId(), m, y)
                    .orElseGet(() -> {
                        PaymentHistory newPh = new PaymentHistory();
                        newPh.setCandidate(c);
                        newPh.setPg(c.getPg());
                        newPh.setPaymentMonth(m);
                        newPh.setPaymentYear(y);
                        newPh.setStatus(PaymentStatus.PENDING);
                        newPh.setPaymentMethod(PaymentMethod.CASH);
                        return newPh;
                    });

            paymentMap.put(c.getCandidateId(), ph);
        }

        model.addAttribute("candidates", candidates);
        model.addAttribute("paymentMap", paymentMap);

        // 🔹 Summary counts for UI: Paid / Partial / Pending / Total
        long total = candidates.size();
        long paidCount = paymentMap.values().stream()
                .filter(p -> p != null && p.getStatus() == PaymentStatus.PAID)
                .count();

        long partialCount = paymentMap.values().stream()
                .filter(p -> p != null && p.getStatus() == PaymentStatus.PARTIAL_PAID)
                .count();

        long pendingCount = total - paidCount - partialCount;

        model.addAttribute("totalCandidates", total);
        model.addAttribute("paidCandidates", paidCount);
        model.addAttribute("partialCandidates", partialCount);
        model.addAttribute("pendingCandidates", pendingCount);

        // ============================================================
        // 🔢 NEW MONTHLY AMOUNT SUMMARY (Advance / Paid / Balance)
        // ============================================================
        if (selectedPg != null) {
            Double totalAdvance = paymentHistoryRepository.getTotalAdvanceAmount(selectedPg, month, year);
            Double totalPaid = paymentHistoryRepository.getTotalAmountPaid(selectedPg, month, year);
            Double totalBalance = paymentHistoryRepository.getTotalBalanceAmount(selectedPg, month, year);

            model.addAttribute("totalAdvanceAmount", totalAdvance);
            model.addAttribute("totalPaidAmount", totalPaid);
            model.addAttribute("totalBalanceAmount", totalBalance);
        }

        return "payment-history";
    }

    // ============================================================
    // 🔷 UPDATE PAYMENT DETAILS
    // Handles POST request to update a candidate's payment details
    // (status, method, amounts, payment date, transaction ID, receipt ID)
    // ============================================================
    @PostMapping("/update")
    public String updatePayment(
            @RequestParam Long candidateId,
            @RequestParam Long pgId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate paymentDate,
            @RequestParam(required = false) Double advance,
            @RequestParam(required = false) Double amountPaid,
            @RequestParam(required = false) Double balance,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String receiptId,
            @RequestParam(required = false) String roomNo
    ) {

        // 🔹 Fetch existing payment record or create new
        PaymentHistory payment = paymentRepo
                .findByCandidate_CandidateIdAndPg_IdAndPaymentMonthAndPaymentYear(candidateId, pgId, month, year)
                .orElse(new PaymentHistory());

        Candidate candidate = candidateRepo.findById(candidateId).orElse(null);
        Pg pg = pgRepo.findById(pgId).orElse(null);

        payment.setCandidate(candidate);
        payment.setPg(pg);
        payment.setPaymentMonth(month);
        payment.setPaymentYear(year);

        // 🔹 Set room number from candidate or request parameter
        if (candidate != null && candidate.getRoomNo() != null) {
            payment.setRoomNo(candidate.getRoomNo());
        } else if (roomNo != null && !roomNo.isBlank()) {
            payment.setRoomNo(roomNo);
        }

        // 🔹 Set payment method if provided
        if (method != null && !method.isBlank()) {
            try {
                payment.setPaymentMethod(PaymentMethod.valueOf(method));
            } catch (Exception ex) {
                // ignore invalid enum
            }
        }

        // 🔹 Set amounts, defaulting to 0.0 if null
        payment.setAdvance(advance != null ? advance : 0.0);
        payment.setAmountPaid(amountPaid != null ? amountPaid : 0.0);
        payment.setBalance(balance != null ? balance : 0.0);

        // 🔹 Set payment status, defaulting to PENDING if invalid
        if (status != null && !status.isBlank()) {
            try {
                payment.setStatus(PaymentStatus.valueOf(status));
            } catch (Exception e) {
                payment.setStatus(PaymentStatus.PENDING);
            }
        }

        // 🔹 Set payment date if provided
        if (paymentDate != null) {
            payment.setPaymentDate(paymentDate);
        }

        // 🔹 Set transaction and receipt IDs, default to empty string if null
        payment.setTransactionId(transactionId != null ? transactionId : "");
        payment.setReceiptId(receiptId != null ? receiptId : "");

        // 🔹 Save payment record
        paymentRepo.save(payment);

        // 🔹 Redirect back to history page with current filters
        String redirectUrl = "/payments/history?pgId=" + pgId + "&month=" + month + "&year=" + year;
        if (roomNo != null && !roomNo.isBlank()) {
            redirectUrl += "&roomNo=" + roomNo;
        }

        return "redirect:" + redirectUrl + "&success=true";
    }

}
