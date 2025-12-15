package com.pgapp.repository;

import com.pgapp.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    /**
     * 🔹 Find an Owner by their email address.
     *     - Useful for login, validation, or email-based lookups.
     *
     * @param email the email of the owner
     * @return an Optional containing the Owner if found, otherwise empty
     */
    Optional<Owner> findByEmail(String email);

    /**
     * 🔹 Find an Owner by their mobile number.
     *     - Useful for mobile-based login or contact validation.
     *
     * @param mobile the mobile number of the owner
     * @return an Optional containing the Owner if found, otherwise empty
     */
    Optional<Owner> findByMobile(String mobile);
} 
