package com.pgapp.service;

import com.pgapp.model.Owner;
import com.pgapp.repository.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpSession;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    // 🔹 Default avatar URL if owner has no custom avatar
    private static final String DEFAULT_AVATAR = "https://www.w3schools.com/w3images/avatar2.png";

    /**
     * 🔹 Upload or update avatar for the owner
     *
     * @param ownerId   the owner ID
     * @param avatarFile the uploaded avatar file
     * @return status message ("SUCCESS", "OWNER_NOT_FOUND", "NO_FILE_SELECTED")
     * @throws IOException if file conversion fails
     */
    public String uploadAvatar(Long ownerId, MultipartFile avatarFile) throws IOException {
        Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);
        if (ownerOpt.isEmpty()) return "OWNER_NOT_FOUND";

        Owner owner = ownerOpt.get();
        if (avatarFile == null || avatarFile.isEmpty()) {
            return "NO_FILE_SELECTED";
        }

        owner.setAvatar(avatarFile.getBytes());
        ownerRepository.save(owner);

        return "SUCCESS";
    }

    /**
     * 🔹 Delete owner's avatar (soft removal)
     *
     * @param ownerId the owner ID
     * @return status message ("SUCCESS" or "OWNER_NOT_FOUND")
     */
    public String deleteAvatar(Long ownerId) {
        Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);
        if (ownerOpt.isEmpty()) return "OWNER_NOT_FOUND";

        Owner owner = ownerOpt.get();
        owner.setAvatar(null);
        ownerRepository.save(owner);

        return "SUCCESS";
    }

    /**
     * 🔹 Get owner's avatar as raw bytes (for <img src>)
     *
     * @param id      optional request parameter for owner ID
     * @param session HTTP session to fallback if id not provided
     * @return ResponseEntity containing avatar bytes or 404 if not found
     */
    @GetMapping("/owner/avatar/image")
    public ResponseEntity<byte[]> getOwnerAvatar(
            @RequestParam(value = "id", required = false) Long id,
            HttpSession session) {

        Long ownerId;

        // 🔹 First use id from request param (e.g., after logout)
        if (id != null) {
            ownerId = id;
        } 
        // 🔹 Fallback to session if logged in
        else if (session.getAttribute("ownerId") != null) {
            ownerId = (Long) session.getAttribute("ownerId");
        } else {
            return ResponseEntity.notFound().build();
        }

        Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);

        // 🔹 Return 404 if owner or avatar not found
        if (ownerOpt.isEmpty() || ownerOpt.get().getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }

        // 🔹 Return avatar bytes with image/jpeg content type
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(ownerOpt.get().getAvatar());
    }
}
