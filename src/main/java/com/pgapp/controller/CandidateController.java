// CandidateController.java with detailed comments added
// NOTE: Functionality unchanged — only comments added.

package com.pgapp.controller;

import com.pgapp.model.Candidate;
import com.pgapp.model.DeletedCandidate;
import com.pgapp.model.Owner;
import com.pgapp.model.Pg;
import com.pgapp.repository.CandidateRepository;
import com.pgapp.repository.DeletedCandidateRepo;
import com.pgapp.repository.OwnerRepository;
import com.pgapp.repository.PgRepository;
import com.pgapp.service.CandidateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    // --- Repositories and Services ---
    private final CandidateRepository candidateRepo;
    private final PgRepository pgRepo;
    private final OwnerRepository ownerRepo;
    private final CandidateService candidateService;

    @Autowired
    private DeletedCandidateRepo deletedCandidateRepo;

    @Autowired
    public CandidateController(CandidateService candidateService, CandidateRepository candidateRepo,
                               PgRepository pgRepo, OwnerRepository ownerRepo) {
        this.candidateService = candidateService;
        this.candidateRepo = candidateRepo;
        this.pgRepo = pgRepo;
        this.ownerRepo = ownerRepo;
    }

    // --------------------------- SHOW NEW CANDIDATE FORM ---------------------------
    @GetMapping("/new")
    public String newCandidateForm(@RequestParam(required = false) Long pgId, HttpSession session, Model model) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null)
            return "redirect:/login"; // Redirect if not logged in

        model.addAttribute("candidate", new Candidate()); // Empty candidate form

        // Load PG list for the owner
        List<Pg> pgs = pgRepo.findByOwnerIdAndDeletedFalse(ownerId);
        model.addAttribute("pgs", pgs);

        // Pre-select PG if provided, otherwise select first PG
        if (pgId != null) {
            model.addAttribute("selectedPgId", pgId);
        } else if (!pgs.isEmpty()) {
            model.addAttribute("selectedPgId", pgs.get(0).getId());
        } else {
            model.addAttribute("selectedPgId", null);
        }

        model.addAttribute("pgId", pgId); // Maintain navigation

        return "candidate-register";
    }

    // --------------------------- SAVE CANDIDATE (ADD OR UPDATE) ---------------------------
    @PostMapping("/save")
    public String saveCandidate(@ModelAttribute Candidate candidate,
                                @RequestParam(value = "selectedPgId", required = false) Long selectedPgId,
                                @RequestParam("photoFile") MultipartFile photoFile,
                                @RequestParam("idProofFile") MultipartFile idProofFile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) throws IOException {

        // ------------------ SESSION VALIDATION ------------------
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        // ------------------ PG VALIDATION ------------------
        Pg pg;
        if (selectedPgId != null) {
            pg = pgRepo.findById(selectedPgId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected PG not found"));
        } else if (candidate.getPg() != null && candidate.getPg().getId() != null) {
            pg = pgRepo.findById(candidate.getPg().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected PG not found"));
        } else {
            model.addAttribute("error", "Please select a PG");
            model.addAttribute("candidate", candidate);
            model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
            return candidate.getCandidateId() == null ? "candidate-register" : "candidate-edit";
        }

        candidate.setPg(pg);

        // ------------------ ROOM NO SAFETY ------------------
        if (candidate.getRoomNo() == null || candidate.getRoomNo().isBlank()) {
            candidate.setRoomNo("NA");
        }

        // ------------------ ADD NEW CANDIDATE ------------------
        if (candidate.getCandidateId() == null) {

            // Files mandatory ONLY for ADD
            if (photoFile.isEmpty() || idProofFile.isEmpty()) {
                model.addAttribute("error", "Photo and ID Proof are mandatory");
                model.addAttribute("candidate", candidate);
                model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
                return "candidate-register";
            }

            // Default joining date
            if (candidate.getJoiningDate() == null) {
                candidate.setJoiningDate(
                    Date.from(LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant())
                );
            }

            candidate.setPhoto(photoFile.getBytes());
            candidate.setIdProof(idProofFile.getBytes());

            candidateRepo.save(candidate);
            redirectAttributes.addFlashAttribute("success", "Candidate added successfully!");
        }

        // ------------------ UPDATE EXISTING CANDIDATE ------------------
        else {
            Candidate existing = candidateRepo.findById(candidate.getCandidateId())
                    .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

            existing.setName(candidate.getName());
            existing.setGender(candidate.getGender());
            existing.setAge(candidate.getAge());
            existing.setDob(candidate.getDob());
            existing.setMobile(candidate.getMobile());
            existing.setEmail(candidate.getEmail());
            existing.setAadhaar(candidate.getAadhaar());
            existing.setGuardianMobile(candidate.getGuardianMobile());
            existing.setPg(candidate.getPg());

            if (candidate.getRoomNo() != null && !candidate.getRoomNo().isBlank()) {
                existing.setRoomNo(candidate.getRoomNo());
            }

            if (candidate.getJoiningDate() != null) {
                existing.setJoiningDate(candidate.getJoiningDate());
            }

            // ✅ FILES OPTIONAL FOR UPDATE
            if (photoFile != null && !photoFile.isEmpty()) {
                existing.setPhoto(photoFile.getBytes());
            }

            if (idProofFile != null && !idProofFile.isEmpty()) {
                existing.setIdProof(idProofFile.getBytes());
            }

            candidateRepo.save(existing);
            redirectAttributes.addFlashAttribute("success", "Candidate details updated successfully!");
        }

        return "redirect:/candidates/list?pgId=" + pg.getId();
    }


    // --------------------------- LIST CANDIDATES ---------------------------
    @GetMapping("/list")
    public String candidateList(@RequestParam(required = false) Long pgId,
                                @RequestParam(value = "deleted", required = false) String deleted,
                                HttpSession session, Model model) {

        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Owner owner = ownerRepo.findById(ownerId).orElse(null);
        if (owner == null) return "redirect:/login";

        model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
        model.addAttribute("ownerName", owner.getOwnerName());
        model.addAttribute("pgId", pgId);
        model.addAttribute("selectedPgId", pgId);

        List<Candidate> candidates;
        String pgName;

        if (pgId != null) {
            candidates = candidateRepo.findByPgIdAndPgOwnerIdOrderByRoomNoAsc(pgId, ownerId);
            Pg selectedPg = pgRepo.findById(pgId).orElse(null);
            pgName = (selectedPg != null) ? selectedPg.getPgName() : "Unknown PG";
        } else {
            candidates = candidateRepo.findByPgOwnerIdOrderByRoomNoAsc(ownerId);
            pgName = "All PGs";
        }

        // Sort room numbers safely
        candidates.sort(Comparator.comparingInt(c -> {
            try { return Integer.parseInt(c.getRoomNo().trim()); }
            catch (Exception e) { return Integer.MAX_VALUE / 2; }
        }));

        model.addAttribute("candidates", candidates);
        model.addAttribute("pgName", pgName);
        if ("success".equals(deleted)) model.addAttribute("deleteSuccess", true);

        return "candidate-list";
    }

    // --------------------------- DISPLAY PHOTOS ---------------------------
    @GetMapping("/photo/{id}")
    @ResponseBody
    public byte[] getPhoto(@PathVariable Long id) {
        return candidateRepo.findById(id).map(Candidate::getPhoto).orElse(null);
    }

    @GetMapping("/idProof/{id}")
    @ResponseBody
    public byte[] getIdProof(@PathVariable Long id) {
        return candidateRepo.findById(id).map(Candidate::getIdProof).orElse(null);
    }

    // --------------------------- EDIT CANDIDATE FORM ---------------------------
    @GetMapping("/edit/{id}")
    public String editCandidate(@PathVariable("id") Long candidateId, HttpSession session, Model model) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        model.addAttribute("candidate", candidateRepo.findById(candidateId).orElseThrow());
        model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));

        return "candidate-edit";
    }

    // --------------------------- DELETE CANDIDATE WITH BACKUP ---------------------------
    @GetMapping("/delete/{id}")
    public String deleteCandidate(@PathVariable("id") Long candidateId,
                                  @RequestParam(required = false) Long pgId,
                                  HttpSession session) {

        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Candidate candidate = candidateRepo.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        // Backup deleted candidate data
        DeletedCandidate deleted = new DeletedCandidate();
        deleted.setCandidateId(candidate.getCandidateId());
        deleted.setName(candidate.getName());
        deleted.setGender(candidate.getGender());
        deleted.setAge(candidate.getAge());

        if (candidate.getDob() != null)
            deleted.setDob(candidate.getDob().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        if (candidate.getJoiningDate() != null)
            deleted.setJoiningDate(candidate.getJoiningDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        deleted.setMobile(candidate.getMobile());
        deleted.setEmail(candidate.getEmail());
        deleted.setRoomNo(candidate.getRoomNo());
        deleted.setAadhaar(candidate.getAadhaar());
        deleted.setGuardianMobile(candidate.getGuardianMobile());
        deleted.setPg(candidate.getPg());
        deleted.setPhoto(candidate.getPhoto());
        deleted.setIdProof(candidate.getIdProof());
        deleted.setDeletedAt(LocalDateTime.now());

        deletedCandidateRepo.save(deleted);
        candidateRepo.deleteById(candidateId);

        return "redirect:/candidates/list?pgId=" + (pgId != null ? pgId : candidate.getPg().getId()) + "&deleted=true";
    }

    // --------------------------- VIEW DELETED CANDIDATES ---------------------------
    @GetMapping("/deleted")
    public String viewDeletedCandidates(@RequestParam(required = false) Long pgId, Model model, HttpSession session) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        List<DeletedCandidate> deletedCandidates;
        String pgName = "All PGs";

        if (pgId != null) {
            deletedCandidates = deletedCandidateRepo.findByPgId(pgId);
            Pg selectedPg = pgRepo.findById(pgId).orElse(null);
            if (selectedPg != null) pgName = selectedPg.getPgName();
        } else {
            deletedCandidates = deletedCandidateRepo.findAll();
        }

        model.addAttribute("deletedCandidates", deletedCandidates);
        model.addAttribute("totalDeleted", deletedCandidates.size());
        model.addAttribute("allPgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
        model.addAttribute("selectedPgId", pgId);
        model.addAttribute("pgName", pgName);

        return "deleted-candidates";
    }

    // --------------------------- DISPLAY DELETED CANDIDATE PHOTOS ---------------------------
    @GetMapping("/deleted/photo/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getDeletedPhoto(@PathVariable Long id) {
        Optional<DeletedCandidate> opt = deletedCandidateRepo.findById(id);
        if (opt.isEmpty() || opt.get().getPhoto() == null) return ResponseEntity.notFound().build();

        byte[] image = opt.get().getPhoto();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        // Detect PNG
        if (image.length > 1 && image[1] == (byte) 0x89)
            headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    @GetMapping("/deleted/idProof/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getDeletedIdProof(@PathVariable Long id) {
        Optional<DeletedCandidate> opt = deletedCandidateRepo.findById(id);
        if (opt.isEmpty() || opt.get().getIdProof() == null) return ResponseEntity.notFound().build();

        byte[] image = opt.get().getIdProof();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        // Detect PNG
        if (image.length > 8 && image[0] == (byte) 0x89 && image[1] == 0x50)
            headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    // --------------------------- LIST CANDIDATES (BACK FROM PAYMENT PAGE) ---------------------------
    @GetMapping("/list/{pgId}")
    public String candidateListByPgId(@PathVariable Long pgId, HttpSession session, Model model) {
        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) return "redirect:/login";

        Owner owner = ownerRepo.findById(ownerId).orElse(null);
        if (owner == null) return "redirect:/login";

        model.addAttribute("pgs", pgRepo.findByOwnerIdAndDeletedFalse(ownerId));
        model.addAttribute("ownerName", owner.getOwnerName());

        Pg selectedPg = pgRepo.findById(pgId).orElse(null);
        if (selectedPg == null) {
            model.addAttribute("error", "PG not found");
            model.addAttribute("candidates", List.of());
            model.addAttribute("pgName", "Unknown PG");
            return "candidate-list";
        }

        List<Candidate> candidates = candidateRepo.findByPgIdAndPgOwnerIdOrderByRoomNoAsc(pgId, ownerId);

        candidates.sort(Comparator.comparingInt(c -> {
            try { return Integer.parseInt(c.getRoomNo().trim()); }
            catch (Exception e) { return Integer.MAX_VALUE / 2; }
        }));

        model.addAttribute("candidates", candidates);
        model.addAttribute("pgName", selectedPg.getPgName());
        model.addAttribute("pgId", pgId);

        return "candidate-list";
    }

    // --------------------------- VACATE DATE UPDATE (AJAX) ---------------------------
    @PostMapping("/updateVacation")
    @ResponseBody
    public Map<String, Object> updateVacation(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long candidateId = Long.valueOf(payload.get("candidateId"));
            String dateStr = payload.get("vacationDate");
            LocalDate vacationDate = (dateStr == null || dateStr.isEmpty()) ? null : LocalDate.parse(dateStr);

            Candidate candidate = candidateService.getCandidateById(candidateId);
            if (candidate != null) {
                candidate.setVacationDate(vacationDate);
                candidateService.saveCandidate(candidate);
                response.put("success", true);
                return response;
            }

            response.put("success", false);
            response.put("message", "Candidate not found");
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
}
