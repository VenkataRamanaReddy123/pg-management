package com.pgapp.repository;

import com.pgapp.model.DeletedCandidate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedCandidateRepo extends JpaRepository<DeletedCandidate, Long> {

    // --- Find all deleted candidates belonging to a specific PG ---
    // Returns a list of DeletedCandidate for the given PG ID
    List<DeletedCandidate> findByPgId(Long pgId);

    // 🔹 You can add more custom queries if needed, for example:
    // find by owner, filter by date, etc.
}
