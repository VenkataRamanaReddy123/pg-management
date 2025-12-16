package com.pgapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.*;

@Entity
@Table(name = "payment_history") // Maps this class to the 'payment_history' table
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    // Many payments can belong to one candidate
    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    // Many payments can belong to one PG
    @ManyToOne
    @JoinColumn(name = "pg_id", nullable = false)
    private Pg pg;

    @Column(name = "room_no", nullable = false) // Room number associated with payment
    private String roomNo;

    // Payment method stored as string (e.g., CASH, ONLINE, UPI)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // Payment status stored as string (PENDING, PAID, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING; // Default to PENDING

    @Column(name = "payment_month", nullable = false) // Month of payment (1-12)
    private int paymentMonth;

    @Column(name = "payment_year", nullable = false) // Year of payment
    private int paymentYear;

    @Column(name = "payment_date") // Actual date when payment was made
    private LocalDate paymentDate;

    @Column(name = "created_at") // Timestamp when record was created
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at") // Timestamp when record was last updated
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== NEW / FIXED FIELDS =====

    @Column(name = "advance_amount", nullable = false) // Advance amount paid by candidate
    private Double advance = 0.0;

    @Column(name = "amount_paid", nullable = false) // Amount paid for this payment entry
    private Double amountPaid = 0.0;

    @Column(name = "balance_amount", nullable = false) // Balance remaining after payment
    private Double balance = 0.0;

    @Column(name = "transaction_id", length = 50) // Transaction ID for online payment
    private String transactionId;

    @Column(name = "receipt_id", length = 50) // Receipt ID for reference
    private String receiptId;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Pg getPg() {
        return pg;
    }

    public void setPg(Pg pg) {
        this.pg = pg;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public int getPaymentMonth() {
        return paymentMonth;
    }

    public void setPaymentMonth(int paymentMonth) {
        this.paymentMonth = paymentMonth;
    }

    public int getPaymentYear() {
        return paymentYear;
    }

    public void setPaymentYear(int paymentYear) {
        this.paymentYear = paymentYear;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getAdvance() {
        return advance;
    }

    public void setAdvance(Double advance) {
        this.advance = advance;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }
    
    @PrePersist
    @PreUpdate
    private void prePersistUpdate() {

        // 🔒 room_no MUST NEVER be null
        if (this.roomNo == null || this.roomNo.isBlank()) {
            this.roomNo = "NA";
        }

        // 🔒 payment_method MUST NEVER be null
        if (this.paymentMethod == null) {
            this.paymentMethod = PaymentMethod.CASH;
        }

        // 🔒 status MUST NEVER be null
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }

        // 🔒 amounts MUST NEVER be null
        if (this.advance == null) this.advance = 0.0;
        if (this.amountPaid == null) this.amountPaid = 0.0;
        if (this.balance == null) this.balance = 0.0;
        // 🔒 timestamps
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
