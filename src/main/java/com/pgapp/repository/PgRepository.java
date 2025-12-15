package com.pgapp.repository;

import com.pgapp.model.Pg;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PgRepository extends JpaRepository<Pg, Long> {

    /**
     * 🔹 Find all PGs for a specific owner that are not deleted.
     *     - Useful for displaying active PGs in the owner's dashboard.
     *
     * @param ownerId the owner ID
     * @return list of active PGs belonging to the owner
     */
    List<Pg> findByOwnerIdAndDeletedFalse(Long ownerId);

    /**
     * 🔹 Find all PGs for a specific owner that are marked as deleted.
     *     - Useful for Trash Bin / Restore functionality.
     *
     * @param ownerId the owner ID
     * @return list of deleted PGs belonging to the owner
     */
    List<Pg> findByOwnerIdAndDeletedTrue(Long ownerId);

    // 🔹 The following commented-out method was replaced by findByOwnerIdAndDeletedFalse/True
    // List<Pg> findByOwnerId(Long ownerId);
} 
