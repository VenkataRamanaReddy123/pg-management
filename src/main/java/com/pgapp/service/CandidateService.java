package com.pgapp.service;

import com.pgapp.model.Candidate;
import com.pgapp.model.DeletedCandidate;
import com.pgapp.repository.CandidateRepository;
import com.pgapp.repository.DeletedCandidateRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final DeletedCandidateRepo deletedCandidateRepo;

    public CandidateService(CandidateRepository candidateRepository,
                            DeletedCandidateRepo deletedCandidateRepo) {
        this.candidateRepository = candidateRepository;
        this.deletedCandidateRepo = deletedCandidateRepo;
    }

    // ----------------- Active Candidate Methods -----------------

    /**
     * 🔹 Retrieve a candidate by ID.
     *
     * @param id candidate ID
     * @return Candidate object if found, otherwise null
     */
    public Candidate getCandidateById(Long id) {
        Optional<Candidate> optional = candidateRepository.findById(id);
        return optional.orElse(null);
    }

    /**
     * 🔹 Save or update a candidate in the database.
     *
     * @param candidate Candidate object
     * @return saved Candidate object
     */
    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    /**
     * 🔹 Get all candidates belonging to a specific PG.
     *
     * @param pgId PG ID
     * @return list of candidates
     */
    public List<Candidate> getCandidatesByPgId(Long pgId) {
        return candidateRepository.findByPg_Id(pgId);
    }

    // ----------------- Deleted Candidate Methods -----------------

    /**
     * 🔹 Retrieve all deleted candidates.
     *
     * @return list of DeletedCandidate objects
     */
    public List<DeletedCandidate> getAllDeletedCandidates() {
        return deletedCandidateRepo.findAll();
    }

    /**
     * 🔹 Retrieve deleted candidates for a specific PG.
     *
     * @param pgId PG ID
     * @return list of DeletedCandidate objects
     */
    public List<DeletedCandidate> getDeletedCandidatesByPgId(Long pgId) {
        return deletedCandidateRepo.findByPgId(pgId);
    }
}
