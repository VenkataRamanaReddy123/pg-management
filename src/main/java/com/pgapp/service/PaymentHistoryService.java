package com.pgapp.service;

import com.pgapp.model.*;
import com.pgapp.repository.PaymentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class PaymentHistoryService {

    @Autowired
    private PaymentHistoryRepository paymentRepo;

    // ===============================================
    // 🔹 Get or create payment entry for a candidate + month + year
    // ===============================================
    @Transactional
    public PaymentHistory saveOrUpdatePayment(Candidate candidate, Pg pg, int month, int year,
                                              PaymentMethod method, PaymentStatus status, LocalDate paymentDate) {

        // 🔹 Fetch existing payment if present, else create new
        PaymentHistory payment = paymentRepo
                .findByCandidate_CandidateIdAndPaymentMonthAndPaymentYear(candidate.getCandidateId(), month, year)
                .orElseGet(() -> {
                    PaymentHistory ph = new PaymentHistory();
                    ph.setCandidate(candidate);
                    ph.setPg(pg);
                    ph.setRoomNo(candidate.getRoomNo());
                    ph.setPaymentMonth(month);
                    ph.setPaymentYear(year);
                    return ph;
                });

        // 🔹 Update payment details
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setPaymentDate(paymentDate);

        return paymentRepo.save(payment);
    }

    // ===============================================
    // 🔹 Fetch all payments for a list of candidates for a specific month/year
    // ===============================================
    public Map<Long, PaymentHistory> getPaymentsForCandidatesAndMonth(List<Candidate> candidates, int month, int year) {
        Map<Long, PaymentHistory> map = new HashMap<>();
        for (Candidate c : candidates) {
            PaymentHistory ph = paymentRepo
                    .findByCandidate_CandidateIdAndPaymentMonthAndPaymentYear(c.getCandidateId(), month, year)
                    .orElse(null); // null if no payment record exists
            map.put(c.getCandidateId(), ph);
        }
        return map;
    }

    // ===============================================
    // 🔹 Count total Paid payments for a PG in a specific month/year
    // ===============================================
    public long countPaid(Pg pg, int month, int year) {
        return paymentRepo.countByPgAndPaymentMonthAndPaymentYearAndStatus(pg, month, year, PaymentStatus.PAID);
    }

    // ===============================================
    // 🔹 Count total Pending payments for a PG in a specific month/year
    // ===============================================
    public long countPending(Pg pg, int month, int year) {
        long total = paymentRepo.countByPgAndPaymentMonthAndPaymentYear(pg, month, year);
        long paid = countPaid(pg, month, year);
        return total - paid; // Pending = Total - Paid
    }

    // ===============================================
    // 🔹 Update existing payment entry by candidateId + month/year
    // ===============================================
    @Transactional
    public PaymentHistory updatePayment(Long candidateId, Long pgId, int month, int year,
                                        PaymentMethod method, PaymentStatus status, LocalDate paymentDate) {

        PaymentHistory ph = paymentRepo
                .findByCandidate_CandidateIdAndPaymentMonthAndPaymentYear(candidateId, month, year)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        // 🔹 Update payment details
        ph.setPaymentMethod(method);
        ph.setStatus(status);
        ph.setPaymentDate(paymentDate);

        return paymentRepo.save(ph);
    }

}
