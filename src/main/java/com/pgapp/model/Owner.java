package com.pgapp.model;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "owner") // Maps this class to the 'owner' table in the database
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @Column(name = "owner_name", nullable = false) // Owner's name, required
    private String ownerName;

    @Column(nullable = false, unique = true) // Email must be unique and not null
    private String email;

    private String mobile; // Owner's mobile number

    @Column(name = "password", nullable = false) // Owner's password, required
    private String password;

    // One-to-many relationship: one owner can have multiple PGs
    // CascadeType.ALL ensures all related PGs are persisted/updated/deleted with the owner
    // orphanRemoval=true ensures PGs are removed if removed from the list
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pg> pgs;

    // ===== Avatar =====
    // New field to store the owner's avatar image in byte format
    @Lob
    @Column(name = "avatar")
    private byte[] avatar;

 // ===== MPIN Login Support =====
    @Column(name = "mpin")
    private String mpin;   // Encrypted 4-digit or 6-digit MPIN
    
    
    private LocalDate trialStartDate;
    private LocalDate trialEndDate;
    private boolean trialExpired;
    private boolean subscribed;
    private LocalDate subscriptionStart;
    private LocalDate subscriptionEnd;
    
    @Column(name = "subscription_plan")
    private String subscriptionPlan;

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
    public LocalDate getTrialStartDate() {
		return trialStartDate;
	}

	public void setTrialStartDate(LocalDate trialStartDate) {
		this.trialStartDate = trialStartDate;
	}

	public LocalDate getTrialEndDate() {
		return trialEndDate;
	}

	public void setTrialEndDate(LocalDate trialEndDate) {
		this.trialEndDate = trialEndDate;
	}

	public boolean isTrialExpired() {
		return trialExpired;
	}

	public void setTrialExpired(boolean trialExpired) {
		this.trialExpired = trialExpired;
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}

	public LocalDate getSubscriptionStart() {
		return subscriptionStart;
	}

	public void setSubscriptionStart(LocalDate subscriptionStart) {
		this.subscriptionStart = subscriptionStart;
	}

	public LocalDate getSubscriptionEnd() {
		return subscriptionEnd;
	}

	public void setSubscriptionEnd(LocalDate subscriptionEnd) {
		this.subscriptionEnd = subscriptionEnd;
	}

	public String getMpin() {
		return mpin;
	}

	public void setMpin(String mpin) {
		this.mpin = mpin;
	}

	public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    // ===== Getters and Setters =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Pg> getPgs() {
        return pgs;
    }

    public void setPgs(List<Pg> pgs) {
        this.pgs = pgs;
    }
}
