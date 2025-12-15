package com.pgapp.repository;

import com.pgapp.model.Pg;
import com.pgapp.model.PaymentHistory;
import com.pgapp.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    /**
     * 🔹 Find all payment histories for a specific PG, month, and year.
     */
    List<PaymentHistory> findByPgAndPaymentMonthAndPaymentYear(Pg pg, int month, int year);

    /**
     * 🔹 Find payment histories for a specific PG filtered by room number (partial match),
     *     month, and year.
     */
    List<PaymentHistory> findByPgAndRoomNoContainingAndPaymentMonthAndPaymentYear(Pg pg, String roomNo, int month,
            int year);

    /**
     * 🔹 Count all payment records for a PG in a specific month and year.
     */
    long countByPgAndPaymentMonthAndPaymentYear(Pg pg, int month, int year);

    /**
     * 🔹 Count all payment records for a PG in a specific month and year with a specific status.
     */
    long countByPgAndPaymentMonthAndPaymentYearAndStatus(Pg pg, int month, int year, PaymentStatus status);

    /**
     * 🔹 Delete all payment histories associated with a specific candidate.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PaymentHistory ph WHERE ph.candidate.candidateId = :candidateId")
    void deleteAllByCandidateId(@Param("candidateId") Long candidateId);

    /**
     * 🔹 Find a payment history record for a candidate in a specific month and year.
     */
    Optional<PaymentHistory> findByCandidate_CandidateIdAndPaymentMonthAndPaymentYear(Long candidateId,
            int paymentMonth, int paymentYear);

    /**
     * 🔹 Retrieve the amount paid for a candidate in a specific month and year.
     */
    @Query("SELECT COALESCE(p.amountPaid,0) FROM PaymentHistory p " +
    	       "WHERE p.candidate.candidateId = :candidateId AND p.paymentMonth = :paymentMonth AND p.paymentYear = :paymentYear")
    	Double findAmountPaid(@Param("candidateId") Long candidateId,
    	                      @Param("paymentMonth") int paymentMonth,
    	                      @Param("paymentYear") int paymentYear);

    /**
     * 🔹 Find payment history by candidate, PG, month, and year.
     */
    Optional<PaymentHistory> findByCandidate_CandidateIdAndPg_IdAndPaymentMonthAndPaymentYear(
            Long candidateId, Long pgId, Integer paymentMonth, Integer paymentYear);

    /**
     * 🔹 Total Advance Amount for selected PG, Month & Year.
     */
    @Query("SELECT COALESCE(SUM(ph.advance),0) FROM PaymentHistory ph " +
           "WHERE ph.pg = :pg AND ph.paymentMonth = :month AND ph.paymentYear = :year")
    Double getTotalAdvanceAmount(@Param("pg") Pg pg,
                                 @Param("month") int month,
                                 @Param("year") int year);

    /**
     * 🔹 Total Amount Paid for selected PG, Month & Year.
     */
    @Query("SELECT COALESCE(SUM(ph.amountPaid),0) FROM PaymentHistory ph " +
           "WHERE ph.pg = :pg AND ph.paymentMonth = :month AND ph.paymentYear = :year")
    Double getTotalAmountPaid(@Param("pg") Pg pg,
                              @Param("month") int month,
                              @Param("year") int year);

    /**
     * 🔹 Total Remaining Balance for selected PG, Month & Year.
     */
    @Query("SELECT COALESCE(SUM(ph.balance),0) FROM PaymentHistory ph " +
           "WHERE ph.pg = :pg AND ph.paymentMonth = :month AND ph.paymentYear = :year")
    Double getTotalBalanceAmount(@Param("pg") Pg pg,
                                 @Param("month") int month,
                                 @Param("year") int year);

    /**
     * 🔹 Retrieve amount paid for a candidate in a specific month/year (alternative method).
     */
    @Query("SELECT COALESCE(p.amountPaid,0) FROM PaymentHistory p " +
           "WHERE p.candidate.candidateId = :candidateId AND p.paymentMonth = :month AND p.paymentYear = :year")
    Double findAmount(@Param("candidateId") Long candidateId,
                      @Param("month") int month,
                      @Param("year") int year);

    /**
     * 🔹 Retrieve advance amount for a candidate in a specific month/year.
     */
    @Query("SELECT COALESCE(p.advance,0) FROM PaymentHistory p " +
           "WHERE p.candidate.candidateId = :candidateId AND p.paymentMonth = :month AND p.paymentYear = :year")
    Double findAdvance(@Param("candidateId") Long candidateId,
                       @Param("month") int month,
                       @Param("year") int year);

    /**
     * 🔹 Retrieve transaction ID for a candidate in a specific month/year.
     */
    @Query("SELECT p.transactionId FROM PaymentHistory p " +
           "WHERE p.candidate.candidateId = :candidateId AND p.paymentMonth = :month AND p.paymentYear = :year")
    String findTransactionId(@Param("candidateId") Long candidateId,
                             @Param("month") int month,
                             @Param("year") int year);

    /**
     * 🔹 Retrieve balance for a candidate in a specific month/year.
     */
    @Query("SELECT COALESCE(p.balance,0) FROM PaymentHistory p " +
           "WHERE p.candidate.candidateId = :candidateId AND p.paymentMonth = :month AND p.paymentYear = :year")
    Double findBalance(@Param("candidateId") Long candidateId,
                       @Param("month") int month,
                       @Param("year") int year);

}
