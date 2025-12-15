package com.pgapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "deleted_candidates") // Maps this class to 'deleted_candidates' table
public class DeletedCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    private Long candidateId; // Original candidate ID from active table
    private String name;      // Candidate's name
    private String gender;    // Gender: male/female/other
    private Integer age;      // Candidate's age
    private LocalDate dob;    // Date of birth (date only, no time)
    private String mobile;    // Mobile number
    private String email;     // Email address
    private String roomNo;    // Room number assigned
    private String aadhaar;   // Aadhaar number
    private LocalDate joiningDate; // Joining date in PG
    private String guardianMobile; // Guardian's mobile number

    // Relationship to PG (many deleted candidates can belong to one PG)
    @ManyToOne
    @JoinColumn(name = "pg_id") // Foreign key to PG table
    private Pg pg;

    @Lob
    private byte[] photo;   // Candidate photo stored as binary data

    @Lob
    private byte[] idProof; // Candidate ID proof (image/pdf) stored as binary data

    // Timestamp when candidate was deleted
    // Default value set to current date and time
    private LocalDateTime deletedAt = LocalDateTime.now();

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
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

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getAadhaar() {
        return aadhaar;
    }

    public void setAadhaar(String aadhaar) {
        this.aadhaar = aadhaar;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public Pg getPg() {
        return pg;
    }

    public void setPg(Pg pg) {
        this.pg = pg;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public byte[] getIdProof() {
        return idProof;
    }

    public void setIdProof(byte[] idProof) {
        this.idProof = idProof;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
