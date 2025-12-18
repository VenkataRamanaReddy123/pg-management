package com.pgapp.controller;

import com.pgapp.model.Owner;
import com.pgapp.model.Pg;
import com.pgapp.repository.OwnerRepository;
import com.pgapp.repository.PgRepository;
import com.pgapp.service.EmailService;
import com.pgapp.service.Msg91Service;
import com.pgapp.service.SmsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Controller
public class AuthController {

	
	   @Value("${app.trial-days}") // default 10 days if not defined
	    private int defaultTrialDays;
	@Autowired
	private OwnerRepository ownerRepo;

	@Autowired
	private PgRepository pgRepo;

	@Autowired
	private SmsService smsService;
	
	@Autowired
	private Msg91Service msg91Service;
	
	  @Autowired
	    private EmailService emailService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	// =====================================================================
	// LOGIN SECTION
	// =====================================================================

	/**
	 * Displays login page. If user already logged in, shows their name on page.
	 */
	@GetMapping({ "/", "/login" })
	public String showLoginPage(
	        HttpSession session,
	        Model model,
	        @CookieValue(value = "ownerId", required = false) String ownerIdCookie) {

	    Long ownerId = (Long) session.getAttribute("ownerId");

	    // ✅ If session missing, try cookie
	    if (ownerId == null && ownerIdCookie != null) {
	        try {
	            ownerId = Long.parseLong(ownerIdCookie);
	        } catch (NumberFormatException ignored) {}
	    }

	    // ✅ Load owner name
	    if (ownerId != null) {
	        ownerRepo.findById(ownerId)
	                 .ifPresent(owner ->
	                     model.addAttribute("ownerName", owner.getOwnerName())
	                 );
	    }

	    return "login";
	}


	/**
	 * Handles login form submission. - Accepts either email or mobile as login
	 * identifier. - Validates password using BCrypt. - Stores owner data in session
	 * + cookies for persistence.
	 */
	@PostMapping("/login")
	public String login(@RequestParam("identifier") String identifier,
			@RequestParam(value = "password", required = false) String password, HttpServletRequest request,
			HttpServletResponse response, HttpSession session, Model model) {

		// 1️⃣ Determine login type: email or mobile
		Optional<Owner> opt = identifier.contains("@") ? ownerRepo.findByEmail(identifier)
				: ownerRepo.findByMobile(identifier);

		if (opt.isEmpty()) {
			model.addAttribute("error", "Invalid credentials.");
			model.addAttribute("identifier", identifier);
			return "login";
		}

		Owner owner = opt.get();

		// 2️⃣ MPIN login
		String mpin = request.getParameter("mpin");
		if (mpin != null && mpin.length() == 4) {
			if (owner.getMpin() != null && passwordEncoder.matches(mpin, owner.getMpin())) {
				session.setAttribute("ownerId", owner.getId());
				setCookies(owner, response);
				return "redirect:/pgs/list";
			} else {
				model.addAttribute("error", "Invalid MPIN.");
				model.addAttribute("identifier", identifier);
				return "login";
			}
		}

		// 3️⃣ Password login
		if (password == null || !passwordEncoder.matches(password, owner.getPassword())) {
			model.addAttribute("error", "Invalid credentials.");
			model.addAttribute("identifier", identifier);
			return "login";
		}

		// 4️⃣ Successful login: save session & cookies
		session.setAttribute("ownerId", owner.getId());
		setCookies(owner, response);

		// ====== TRIAL DAYS CALCULATION ======
		// ====== ✅ SAFE TRIAL / SUBSCRIPTION DAYS CALCULATION ======
		LocalDate today = LocalDate.now();

		// ✅ If user has an active subscription, ignore trial completely
		if (owner.getSubscriptionEnd() != null) {

		    long remainingDays = ChronoUnit.DAYS.between(today, owner.getSubscriptionEnd());
		    if (remainingDays < 0) remainingDays = 0;

		    session.setAttribute("remainingDays", remainingDays);
		    session.setAttribute("subscriptionPlan", owner.getSubscriptionPlan());

		}
		// ✅ Only check trial when no subscription exists
		else if (owner.getTrialEndDate() != null) {

		    long remainingDays = ChronoUnit.DAYS.between(today, owner.getTrialEndDate());
		    if (remainingDays < 0) remainingDays = 0;

		    // ✅ Update trialExpired flag ONLY for real trial users
		    if (!owner.isTrialExpired() && remainingDays == 0) {
		        owner.setTrialExpired(true);
		        ownerRepo.save(owner);
		    }

		    session.setAttribute("remainingDays", remainingDays);
		    session.setAttribute("subscriptionPlan", owner.getSubscriptionPlan());
		}


		
		return "redirect:/pgs/list";
	}

	// ✅ Helper method to set cookies
	private void setCookies(Owner owner, HttpServletResponse response) {

	    // ✅ Store ONLY ownerId in cookie (safe, numeric, RFC compliant)
	    Cookie idCookie = new Cookie("ownerId", String.valueOf(owner.getId()));
	    idCookie.setHttpOnly(false); // used by UI / JS if needed
	    idCookie.setPath("/");
	    idCookie.setMaxAge(24 * 60 * 60); // 1 day
	    response.addCookie(idCookie);

	}



	// =====================================================================
	// LOGOUT
	// =====================================================================

	/**
	 * Logouts user by clearing session but keeps name/id in cookie to show on login
	 * page.
	 */
	@GetMapping("/logout")
	public String logout(HttpSession session, HttpServletResponse response) {

	    // 1️⃣ Get owner from session BEFORE invalidating
	    Long ownerId = (Long) session.getAttribute("ownerId");
	    Owner owner = null;

	    if (ownerId != null) {
	        owner = ownerRepo.findById(ownerId).orElse(null);
	    }

	    // 2️⃣ Clear session completely
	    session.invalidate();
	    // 4️⃣ Redirect back to login
	    return "redirect:/login";
	}


	// =====================================================================
	// REGISTRATION SECTION
	// =====================================================================

	/** Shows Owner Registration Form */
	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		model.addAttribute("owner", new Owner());
		return "owner-register";
	}

	/**
	 * Handles owner registration: - Validates password match - Checks duplicate
	 * email/mobile - Saves Owner & PG record
	 */
	@PostMapping("/register")
	public String registerOwner(@RequestParam String ownerName,
	                            @RequestParam String pgName,
	                            @RequestParam String pgAddress,
	                            @RequestParam String mobile,
	                            @RequestParam String email,
	                            @RequestParam String password,
	                            @RequestParam String confirmPassword,
	                            @RequestParam(required = false) String mpin,
	                            RedirectAttributes redirectAttributes) {

		  // ---------------------------
	    // 1. Validate Password Match
	    // ---------------------------
	    if (!password.equals(confirmPassword)) {
	        redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
	        saveUserInput(redirectAttributes, ownerName, pgName, pgAddress, mobile, email);
	        return "redirect:/register";
	    }

	    // ---------------------------
	    // 2. Validate Password Strength
	    // Minimum 8, max 16 chars, uppercase, lowercase, number, special char
	    // ---------------------------
	    if (!isValidPassword(password)) {
	        redirectAttributes.addFlashAttribute("error", 
	            "Password must be 8-16 chars, include uppercase, lowercase, number & special char.");
	        saveUserInput(redirectAttributes, ownerName, pgName, pgAddress, mobile, email);
	        return "redirect:/register";
	    }
	    
	    
	    // Check duplicate email/mobile
	    if (ownerRepo.findByEmail(email).isPresent() || ownerRepo.findByMobile(mobile).isPresent()) {
	        redirectAttributes.addFlashAttribute("error", "Owner already exists with this email or mobile.");
	        saveUserInput(redirectAttributes, ownerName, pgName, pgAddress, mobile, email);
	        return "redirect:/register";
	    }

	    // Save Owner
	    Owner owner = new Owner();
	    owner.setOwnerName(ownerName);
	    owner.setMobile(mobile);
	    owner.setEmail(email);
	    owner.setPassword(passwordEncoder.encode(password));

	    // Set trial period
	    owner.setTrialStartDate(LocalDate.now());
	    owner.setTrialEndDate(LocalDate.now().plusDays(defaultTrialDays));
	    owner.setTrialExpired(false);
	    owner.setSubscribed(false);
	    owner.setSubscriptionPlan("TRIAL");

	    // Save MPIN if entered
	    if (mpin != null && mpin.matches("\\d{4}")) {
	        owner.setMpin(passwordEncoder.encode(mpin));
	    }

	    ownerRepo.save(owner);

	    // Save PG
	    Pg pg = new Pg();
	    pg.setPgName(pgName);
	    pg.setAddress(pgAddress);
	    pg.setMobile(mobile);
	    pg.setEmail(email);
	    pg.setOwner(owner);
	    pgRepo.save(pg);

	    // ===========================
	    // Send Registration Email
	    // ===========================
	 // After saving owner & PG
	    emailService.sendRegistrationEmail(email, ownerName, pgName, pgAddress, mobile,password,mpin);

	    redirectAttributes.addFlashAttribute("success", "Registration Successful!");
	    return "redirect:/login?success=true";
	}

	// Helper: Password Strength Check
	// ---------------------------
	private boolean isValidPassword(String password) {
	    String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,16}$";
	    return password.matches(pattern);
	}
	
	/** Helper to preserve user-input on failure */
	private void saveUserInput(RedirectAttributes redirectAttributes, String ownerName, String pgName, String pgAddress,
			String mobile, String email) {
		redirectAttributes.addFlashAttribute("ownerName", ownerName);
		redirectAttributes.addFlashAttribute("pgName", pgName);
		redirectAttributes.addFlashAttribute("pgAddress", pgAddress);
		redirectAttributes.addFlashAttribute("mobile", mobile);
		redirectAttributes.addFlashAttribute("email", email);
	}

	// =====================================================================
	// PG MANAGEMENT SECTION
	// =====================================================================

	/** Displays list of PGs for logged-in owner */
	@GetMapping("/pgs")
	public String listPgs(HttpSession session, Model model) {

		Long ownerId = (Long) session.getAttribute("ownerId");
		if (ownerId == null)
			return "redirect:/login";
		// ✅ Fetch logged-in owner name from DB
	    ownerRepo.findById(ownerId)
	             .ifPresent(owner ->
	                 model.addAttribute("ownerName", owner.getOwnerName()));
		model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
		return "pg-list";
	}

	/** Add a new PG under logged-in owner */
	@PostMapping("/pgs/add")
	public String addPg(@RequestParam String pgName, @RequestParam String address, @RequestParam String mobile,
			@RequestParam String email, HttpSession session) {

		Long ownerId = (Long) session.getAttribute("ownerId");
		if (ownerId == null)
			return "redirect:/login";

		Owner owner = ownerRepo.findById(ownerId).orElse(null);
		if (owner == null)
			return "redirect:/login";

		Pg pg = new Pg();
		pg.setPgName(pgName);
		pg.setAddress(address);
		pg.setMobile(mobile);
		pg.setEmail(email);
		pg.setOwner(owner);
		pgRepo.save(pg);

		return "redirect:/pgs";
	}

	// =====================================================================
	// FORGOT PASSWORD SECTION
	// =====================================================================

	/** Show change password page */
	@GetMapping("/change-password")
	public String showChangePasswordPage() {
		return "change-password";
	}

	@PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(@RequestParam String mobile, HttpSession session) {
        Optional<Owner> ownerOpt = ownerRepo.findByMobile(mobile);
        if (ownerOpt.isEmpty()) return "No account found with that mobile number.";

        String otp = String.format("%04d", new Random().nextInt(10000));
        session.setAttribute("otp", otp);
        session.setAttribute("otpMobile", mobile);

        // Send OTP via Email
        emailService.sendOtpEmail(ownerOpt.get().getEmail(), otp, "Password Reset");
        return "OTP sent successfully to your registered email: " + ownerOpt.get().getEmail();
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public String verifyOtp(@RequestParam String mobile, @RequestParam String otp, HttpSession session) {
        String sessionOtp = (String) session.getAttribute("otp");
        String otpMobile = (String) session.getAttribute("otpMobile");
        if (sessionOtp == null || otpMobile == null || !otpMobile.equals(mobile)) return "invalid";
        return sessionOtp.equals(otp) ? "success" : "invalid";
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public String resetPassword(@RequestParam String mobile, @RequestParam String otp,
                                @RequestParam String newPassword, @RequestParam String confirmPassword,
                                HttpSession session) {

        String sessionOtp = (String) session.getAttribute("otp");
        String otpMobile = (String) session.getAttribute("otpMobile");

        if (sessionOtp == null || otpMobile == null || !otpMobile.equals(mobile))
            return "OTP expired or invalid session. Please resend OTP.";

        if (!sessionOtp.equals(otp)) return "Invalid OTP. Please try again.";
        if (!newPassword.equals(confirmPassword)) return "Passwords do not match.";

        // Server-side password length validation (8-16 characters)
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 16) {
            return "Password must be between 8 and 16 characters.";
        }
        
        Optional<Owner> ownerOpt = ownerRepo.findByMobile(mobile);
        if (ownerOpt.isEmpty()) return "No user found with that mobile number.";

        Owner owner = ownerOpt.get();
        owner.setPassword(passwordEncoder.encode(newPassword));
        ownerRepo.save(owner);
        // ✅ Send email notifying password change (masked password)
        emailService.sendCredentialChangeEmail(owner.getEmail(), owner.getOwnerName(), newPassword, null);
        session.removeAttribute("otp");
        session.removeAttribute("otpMobile");

        return "Password updated successfully! Please login again.";
    }

    // ===================== MPIN RESET =====================
    @GetMapping("/change-mpin")
    public String showChangeMpinPage() {
        return "change-mpin";
    }

    @PostMapping("/send-otp-mpin")
    @ResponseBody
    public String sendOtpForMpin(@RequestParam String mobile, HttpSession session) {
        Optional<Owner> ownerOpt = ownerRepo.findByMobile(mobile);
        if (ownerOpt.isEmpty()) return "No account found with that mobile number.";

        String otp = String.format("%04d", new Random().nextInt(10000));
        session.setAttribute("otpMpin", otp);
        session.setAttribute("otpMobileMpin", mobile);

        // ✅ Send OTP via Email
        emailService.sendOtpEmail(ownerOpt.get().getEmail(), otp, "MPIN Reset");
        return "OTP sent successfully to your registered email: " + ownerOpt.get().getEmail();
    }

    @PostMapping("/verify-otp-mpin")
    @ResponseBody
    public String verifyOtpForMpin(@RequestParam String mobile, @RequestParam String otp, HttpSession session) {
        String sessionOtp = (String) session.getAttribute("otpMpin");
        String otpMobile = (String) session.getAttribute("otpMobileMpin");
        if (sessionOtp == null || otpMobile == null || !otpMobile.equals(mobile)) return "invalid";
        return sessionOtp.equals(otp) ? "success" : "invalid";
    }

    @PostMapping("/reset-mpin")
    @ResponseBody
    public String resetMpin(@RequestParam String mobile, @RequestParam String otp,
                            @RequestParam String newMpin, @RequestParam String confirmMpin,
                            HttpSession session) {

        String sessionOtp = (String) session.getAttribute("otpMpin");
        String otpMobile = (String) session.getAttribute("otpMobileMpin");

        if (sessionOtp == null || otpMobile == null || !otpMobile.equals(mobile))
            return "OTP expired or invalid session. Please resend OTP.";

        if (!sessionOtp.equals(otp)) return "Invalid OTP. Please try again.";
        if (!newMpin.equals(confirmMpin)) return "MPINs do not match.";
        if (!newMpin.matches("\\d{4}")) return "MPIN must be a 4-digit number.";

        Optional<Owner> ownerOpt = ownerRepo.findByMobile(mobile);
        if (ownerOpt.isEmpty()) return "No user found with that mobile number.";

        Owner owner = ownerOpt.get();
        owner.setMpin(passwordEncoder.encode(newMpin));
        ownerRepo.save(owner);

        // ✅ Send email notifying MPIN change (masked MPIN)
        emailService.sendCredentialChangeEmail(owner.getEmail(), owner.getOwnerName(), null, newMpin);
        session.removeAttribute("otpMpin");
        session.removeAttribute("otpMobileMpin");

        return "MPIN updated successfully! You can now login using your new MPIN.";
    }


	// =====================================================================
	// AVATAR / PROFILE PICTURE
	// =====================================================================

	/**
	 * Uploads Avatar (Profile Picture) for Owner. Accepts image as MultipartFile.
	 */
	@PostMapping("/upload-avatar")
	@ResponseBody
	public String uploadAvatar(@RequestParam("avatar") MultipartFile avatarFile, HttpSession session,
			@CookieValue(value = "ownerId", required = false) String ownerIdCookie) throws IOException {

		Long ownerId = resolveOwnerId(session, ownerIdCookie);
		if (ownerId == null)
			return "NOT_LOGGED_IN";

		Optional<Owner> ownerOpt = ownerRepo.findById(ownerId);
		if (ownerOpt.isEmpty())
			return "OWNER_NOT_FOUND";

		// Save avatar bytes to DB
		Owner owner = ownerOpt.get();
		owner.setAvatar(avatarFile.getBytes());
		ownerRepo.save(owner);

		return "SUCCESS";
	}

	/**
	 * Deletes avatar of logged-in owner.
	 */
	@PostMapping("/delete-avatar")
	@ResponseBody
	public String deleteAvatar(HttpSession session,
			@CookieValue(value = "ownerId", required = false) String ownerIdCookie) {

		Long ownerId = resolveOwnerId(session, ownerIdCookie);
		if (ownerId == null)
			return "NOT_LOGGED_IN";

		Optional<Owner> ownerOpt = ownerRepo.findById(ownerId);
		if (ownerOpt.isEmpty())
			return "OWNER_NOT_FOUND";

		Owner owner = ownerOpt.get();
		owner.setAvatar(null);
		ownerRepo.save(owner);

		return "SUCCESS";
	}

	/**
	 * Returns avatar image (as byte[]) for owner. Used by <img> tag to render
	 * profile picture.
	 */
	@GetMapping("/owner/avatar/image")
	public ResponseEntity<byte[]> getOwnerAvatar(@RequestParam(value = "id", required = false) Long id,
			HttpSession session, @CookieValue(value = "ownerId", required = false) String ownerIdCookie) {

		Long ownerId = (id != null) ? id : resolveOwnerId(session, ownerIdCookie);
		if (ownerId == null)
			return ResponseEntity.notFound().build();

		Optional<Owner> opt = ownerRepo.findById(ownerId);
		if (opt.isEmpty() || opt.get().getAvatar() == null)
			return ResponseEntity.notFound().build();

		return ResponseEntity.ok().header("Content-Type", "image/jpeg").body(opt.get().getAvatar());
	}

	/** Helper to safely retrieve ownerId from session or cookie */
	private Long resolveOwnerId(HttpSession session, String cookieValue) {
		Long ownerId = (Long) session.getAttribute("ownerId");

		if (ownerId == null && cookieValue != null) {
			try {
				return Long.parseLong(cookieValue);
			} catch (Exception ignored) {
			}
		}
		return ownerId;
	}
	
}
