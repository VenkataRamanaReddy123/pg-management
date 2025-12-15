package com.pgapp.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pg") // Maps this class to the 'pg' table
public class Pg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @Column(name = "pg_name", nullable = false) // Name of the PG, required
    private String pgName;

    private String address; // PG address
    private String mobile;  // PG contact number
    private String email;   // PG email address

    // ===== Soft Delete =====
    // Indicates if the PG is deleted (soft delete)
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // ===== Owner Relationship =====
    // Many PGs can belong to one owner
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    // ===== Candidates Relationship =====
    // One PG can have multiple candidates
    // CascadeType.ALL propagates CRUD operations to candidates
    // orphanRemoval=true removes candidates if removed from this list
    @OneToMany(mappedBy = "pg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidate> candidates = new ArrayList<>();

    // Monthly rent for the PG
    @Column(name = "monthly_rent")
    private Double monthlyRent = 0.0; // default to 0.0

    public Double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(Double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    // ===== Getters & Setters =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPgName() {
        return pgName;
    }

    public void setPgName(String pgName) {
        this.pgName = pgName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // ===== Safe setter for candidates =====
    // Clears existing list and adds new candidates using addCandidate()
    // Avoids issues with orphan removal in JPA
    public void setCandidates(List<Candidate> candidates) {
        this.candidates.clear();
        if (candidates != null) {
            for (Candidate c : candidates) {
                addCandidate(c);
            }
        }
    }

    // ===== Helper methods for bidirectional relationship =====
    // Adds a candidate and sets its PG reference to this
    public void addCandidate(Candidate candidate) {
        candidates.add(candidate);
        candidate.setPg(this);
    }

    // Removes a candidate and clears its PG reference
    public void removeCandidate(Candidate candidate) {
        candidates.remove(candidate);
        candidate.setPg(null);
    }
}
