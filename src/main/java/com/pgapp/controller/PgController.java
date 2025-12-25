package com.pgapp.controller;

import com.pgapp.model.Pg;
import com.pgapp.model.Owner;
import com.pgapp.repository.PgRepository;
import com.pgapp.repository.OwnerRepository;
import com.pgapp.repository.CandidateRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;


/**
 * @author Venkata RamanaReddy Kokatam
 * Date: 01-Dec-2025
 */

@Controller
@RequestMapping("/pgs")
public class PgController {

    @Autowired
    private PgRepository pgRepository;

    private final PgRepository pgRepo;
    private final OwnerRepository ownerRepo;
    private final CandidateRepository candidateRepo;

    // ✔ Constructor Injection (Recommended for testability & immutability)
    public PgController(PgRepository pgRepo, OwnerRepository ownerRepo, CandidateRepository candidateRepo) {
        this.pgRepo = pgRepo;
        this.ownerRepo = ownerRepo;
        this.candidateRepo = candidateRepo;
    }

    /**
     * 📌 Displays all PGs belonging to the logged-in owner.
     *     - Only non-deleted PGs are shown.
     *     - Owner name is also displayed in UI header.
     */
    @GetMapping("/list")
    public String viewPgList(HttpSession session, Model model) {

        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Owner owner = ownerRepo.findById(ownerId).orElse(null);
        if (owner == null) return "redirect:/login";

        // PG LIST
        List<Pg> pgList = pgRepo.findByOwnerIdAndDeletedFalse(ownerId);

        model.addAttribute("pgs", pgList);
        model.addAttribute("pg", new Pg());
        model.addAttribute("ownerName", owner.getOwnerName());

     // ============================
     // ✅ FIXED TRIAL / SUBSCRIPTION CHECK (SAFE VERSION)
     // ============================
     LocalDate today = LocalDate.now();

     boolean trialExpired = false;
     boolean subscriptionExpired = false;
     boolean isTrial = false;

     LocalDate endDate = null;

     // ✅ Priority 1: Subscription (ignore trial if exists)
     if (owner.getSubscriptionEnd() != null) {

         endDate = owner.getSubscriptionEnd();
         isTrial = false;

         // only subscription matters
         subscriptionExpired = today.isAfter(endDate);

     }
     // ✅ Priority 2: Trial (only if no subscription)
     else if (owner.getTrialEndDate() != null) {

         endDate = owner.getTrialEndDate();
         isTrial = true;

         trialExpired = today.isAfter(endDate);
     }

     model.addAttribute("trialExpired", trialExpired);
     model.addAttribute("subscriptionExpired", subscriptionExpired);
     model.addAttribute("isTrial", isTrial);
     boolean hasActiveSubscription = false;

  // subscription exists AND not expired
  if (owner.getSubscriptionEnd() != null && !subscriptionExpired) {
      hasActiveSubscription = true;
  }

  model.addAttribute("hasActiveSubscription", hasActiveSubscription);


        // ============================
        // 🔥 Remaining Days + Date Format
        // ============================
        Long remainingDays = null;

        if (endDate != null) {
        	// inclusive calculation – today is still usable
        	remainingDays = ChronoUnit.DAYS.between(today, endDate.plusDays(1));
            if (remainingDays < 0) remainingDays = 0L;

            // Format DD-MM-YYYY
            String formattedDate = endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            model.addAttribute("formattedEndDate", formattedDate);
        }

        model.addAttribute("remainingDays", remainingDays);
        model.addAttribute("subscriptionPlan", owner.getSubscriptionPlan());
        return "pg-list";
    }



    /**
     * 💾 Creates a new PG or Updates an existing PG.
     *    - Automatically links PG with the logged-in owner.
     *    - While updating, existing candidates under that PG are preserved.
     */
    @PostMapping("/save")
    public String savePg(@ModelAttribute Pg pg, HttpSession session) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Owner owner = ownerRepo.findById(ownerId).orElseThrow();
        pg.setOwner(owner); // 🔹 Assign logged-in owner

        boolean isEdit = pg.getId() != null;

        // 🔹 Preserve candidate list during edit to avoid data loss
        if (isEdit) {
            Pg existingPg = pgRepo.findById(pg.getId())
                    .orElseThrow(() -> new IllegalArgumentException("PG not found"));

            pg.setCandidates(existingPg.getCandidates());
        }

        // 🔹 Save new or updated PG
        pgRepo.save(pg);
        // ⭐ FIX — sync updated PG email to OWNER table
        if (pg.getEmail() != null && !pg.getEmail().equals(owner.getEmail())) {
            owner.setEmail(pg.getEmail());
            ownerRepo.save(owner);
        }
        // 🔹 Redirect to list page with success message flag
        return "redirect:/pgs/list?success=" + (isEdit ? "edit" : "add");
    }

    /**
     * 👥 Shows all Candidates under a specific PG.
     *     - Ensures owner security (only owners can view their PG).
     *     - Provides PG ID for navigation to Payment History page.
     */
    @GetMapping("/{pgId}/candidates")
    public String viewCandidatesByPg(@PathVariable Long pgId, HttpSession session, Model model) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Pg pg = pgRepo.findById(pgId).orElse(null);

        // 🔹 Check PG exists and belongs to logged-in owner
        if (pg != null && pg.getOwner().getId().equals(ownerId)) {

            model.addAttribute("pg", pg);
            model.addAttribute("pgId", pg.getId()); // Needed for payment redirect
            // 🔹 Fetch candidates ordered by room number
            model.addAttribute("candidates",
                    candidateRepo.findByPgIdAndPgOwnerIdOrderByRoomNoAsc(pgId, ownerId));

        } else {
            model.addAttribute("error", "PG not found or unauthorized access");
            model.addAttribute("candidates", List.of());
        }

        return "candidate-list";
    }

    /**
     * 🗑️ Soft Deletes a PG (keeps data in DB).
     *     - PG will no longer display in the active PG list.
     */
    @GetMapping("/delete")
    public String softDeletePg(@RequestParam Long id, RedirectAttributes redirect) {
        Pg pg = pgRepo.findById(id).orElse(null);

        if (pg != null) {
            pg.setDeleted(true);  // 🔹 Logical deletion
            pgRepo.save(pg);
            redirect.addFlashAttribute("deleted", pg.getPgName());
        } else {
            redirect.addFlashAttribute("error", "PG not found");
        }

        return "redirect:/pgs/list";
    }

    /**
     * ♻️ Displays list of deleted PGs (Trash Bin View).
     */
    @GetMapping("/deleted-list")
    public String viewDeletedPgList(HttpSession session, Model model) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        // 🔹 Fetch PGs marked as deleted
        model.addAttribute("deletedPgs",
                pgRepo.findByOwnerIdAndDeletedTrue(ownerId));

        return "pg-deleted-list";
    }

    /**
     * 🔁 Restores a previously soft-deleted PG back to active list.
     */
    @GetMapping("/restore")
    public String restorePg(@RequestParam Long id) {
        Pg pg = pgRepo.findById(id).orElse(null);

        if (pg != null) {
            pg.setDeleted(false);  // 🔹 Restore PG
            pgRepo.save(pg);
            return "redirect:/pgs/list?restored=" + pg.getPgName();
        }
        return "redirect:/pgs/list?error=true";
    }
}
