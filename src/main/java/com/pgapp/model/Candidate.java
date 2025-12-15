package com.pgapp.model;

import javax.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "candidate_details") // Maps this class to the 'candidate_details' table
public class Candidate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
	private Long candidateId;

	private String name; // Candidate's full name
	private String gender; // Gender: male/female/other
	private Integer age; // Candidate's age
	private String mobile; // Mobile number (10 digits)
	private String email; // Email address
	private String roomNo; // Room number assigned in PG
	private String aadhaar; // Aadhaar number (12 digits)
	private boolean deleted = false; // Soft delete flag

	@Column(name = "vacation_date")
	private LocalDate vacationDate; // Date when candidate vacates PG

	// Getter and Setter for vacationDate
	public LocalDate getVacationDate() {
		return vacationDate;
	}

	public void setVacationDate(LocalDate vacationDate) {
		this.vacationDate = vacationDate;
	}

	// Getter and Setter for soft delete
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date dob; // Date of birth

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date joiningDate; // Date of joining PG

	private String guardianMobile; // Guardian's mobile number
	private String address; // Candidate's address

	@Lob
	private byte[] photo; // Candidate photo stored as byte array in DB

	@Lob
	private byte[] idProof; // Candidate ID proof (image/pdf) stored as byte array

	// ===== PG Relationship =====
	// Many candidates can belong to one PG
	@ManyToOne
	@JoinColumn(name = "pg_id", nullable = false)
	private Pg pg;

	// ===== Getters & Setters =====

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

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
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

	public Date getJoiningDate() {
		return joiningDate;
	}

	public void setJoiningDate(Date joiningDate) {
		this.joiningDate = joiningDate;
	}

	public String getGuardianMobile() {
		return guardianMobile;
	}

	public void setGuardianMobile(String guardianMobile) {
		this.guardianMobile = guardianMobile;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
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

	public Pg getPg() {
		return pg;
	}

	public void setPg(Pg pg) {
		this.pg = pg;
	}
}
