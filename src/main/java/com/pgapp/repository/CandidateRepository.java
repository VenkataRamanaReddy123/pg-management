package com.pgapp.repository;

import com.pgapp.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

	// --- Find candidates belonging to any of the specified PG IDs ---
	List<Candidate> findByPgIdIn(List<Long> pgIds);

	// --- Find all candidates for a specific owner, ordered by room number
	// ascending ---
	@Query("SELECT c FROM Candidate c WHERE c.pg.owner.id = :ownerId ORDER BY c.roomNo ASC")
	List<Candidate> findByOwnerId(@Param("ownerId") Long ownerId);

	// --- Find candidates for a specific PG, ordered by room number ascending ---
	@Query("SELECT c FROM Candidate c WHERE c.pg.id = :pgId ORDER BY c.roomNo ASC")
	List<Candidate> findByPgIdOrderByRoomNoAsc(@Param("pgId") Long pgId);

	// --- Find candidates for a specific PG AND owner, ordered by room number
	// ascending ---
	@Query("SELECT c FROM Candidate c WHERE c.pg.id = :pgId AND c.pg.owner.id = :ownerId ORDER BY c.roomNo ASC")
	List<Candidate> findByPgIdAndPgOwnerIdOrderByRoomNoAsc(@Param("pgId") Long pgId, @Param("ownerId") Long ownerId);

	// --- Alternative method: find all candidates for a specific owner, ordered by
	// room number ascending ---
	@Query("SELECT c FROM Candidate c WHERE c.pg.owner.id = :ownerId ORDER BY c.roomNo ASC")
	List<Candidate> findByPgOwnerIdOrderByRoomNoAsc(@Param("ownerId") Long ownerId);

	// --- Fetch all candidates, ordered by room number ascending ---
	@Query("SELECT c FROM Candidate c ORDER BY c.roomNo ASC")
	List<Candidate> findAllOrderByRoomNoAsc();

	// --- Derived query: find candidates by PG ID ---
	List<Candidate> findByPg_Id(Long pgId);

	// --- Find all candidates marked as deleted ---
	List<Candidate> findByDeletedTrue();

	// --- Find all deleted candidates for a specific PG ---
	List<Candidate> findByPgIdAndDeletedTrue(Long pgId);

}
