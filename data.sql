-- ======================================================
-- Table: owner
-- Stores PG owner information
-- ======================================================
CREATE TABLE IF NOT EXISTS owner (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- Primary key
    owner_name VARCHAR(100) NOT NULL,             -- Owner's name
    email VARCHAR(100) NOT NULL UNIQUE,           -- Unique email
    mobile VARCHAR(15) DEFAULT NULL,             -- Optional mobile
    password VARCHAR(255) NOT NULL,              -- Password (hashed in application)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Auto-set creation timestamp
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- Auto-update timestamp
    avatar LONGBLOB DEFAULT NULL                 -- Optional profile picture
);

-- ======================================================
-- Table: pg
-- Stores PG details linked to an owner
-- ======================================================
CREATE TABLE IF NOT EXISTS pg (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pg_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) DEFAULT NULL,
    mobile VARCHAR(15) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    owner_id BIGINT NOT NULL,                     -- Foreign key reference to owner
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BIT(1) NOT NULL DEFAULT b'0',     -- Soft delete flag
    monthly_rent DOUBLE DEFAULT NULL,
    CONSTRAINT fk_pg_owner FOREIGN KEY (owner_id) REFERENCES owner(id) ON DELETE CASCADE
);

-- ======================================================
-- Table: candidate_details
-- Stores details of PG candidates
-- ======================================================
CREATE TABLE IF NOT EXISTS candidate_details (
    candidate_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) DEFAULT NULL,
    gender VARCHAR(10) DEFAULT NULL,
    age INT DEFAULT NULL,
    dob DATE DEFAULT NULL,
    mobile VARCHAR(15) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    room_no VARCHAR(20) DEFAULT NULL,
    aadhaar VARCHAR(20) DEFAULT NULL,
    joining_date DATE DEFAULT NULL,
    guardian_mobile VARCHAR(15) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    photo LONGBLOB DEFAULT NULL,                  -- Candidate photo
    pg_id BIGINT NOT NULL,                        -- Foreign key reference to PG
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_proof LONGBLOB DEFAULT NULL,               -- ID proof document
    deleted BIT(1) NOT NULL DEFAULT b'0',         -- Soft delete flag
    vacation_date DATE DEFAULT NULL,
    CONSTRAINT fk_candidate_pg FOREIGN KEY (pg_id) REFERENCES pg(id) ON DELETE CASCADE
);

-- ======================================================
-- Table: deleted_candidates
-- Stores data of candidates who are deleted
-- ======================================================
CREATE TABLE IF NOT EXISTS deleted_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT DEFAULT NULL,             -- Original candidate_id
    name VARCHAR(255) DEFAULT NULL,
    gender VARCHAR(10) DEFAULT NULL,
    age INT DEFAULT NULL,
    dob DATE DEFAULT NULL,
    mobile VARCHAR(20) DEFAULT NULL,
    email VARCHAR(255) DEFAULT NULL,
    room_no VARCHAR(10) DEFAULT NULL,
    aadhaar VARCHAR(20) DEFAULT NULL,
    joining_date DATE DEFAULT NULL,
    guardian_mobile VARCHAR(20) DEFAULT NULL,
    pg_id BIGINT DEFAULT NULL,                    -- PG reference for historical record
    photo LONGBLOB DEFAULT NULL,
    id_proof LONGBLOB DEFAULT NULL,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deleted_candidate FOREIGN KEY (pg_id) REFERENCES pg(id) ON DELETE CASCADE
);

-- ======================================================
-- Table: payment_history
-- Stores payment records for candidates
-- ======================================================
CREATE TABLE IF NOT EXISTS payment_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,                 -- Candidate reference
    pg_id BIGINT NOT NULL,                        -- PG reference
    room_no VARCHAR(20) NOT NULL,
    payment_method ENUM('PHONEPAY','GPAY','PAYTM','CRED','CASH') NOT NULL,
    status ENUM('PENDING','PARTIAL_PAID','PAID') NOT NULL DEFAULT 'PENDING',
    payment_month INT NOT NULL,
    payment_year INT NOT NULL,
    payment_date DATE DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    advance_amount DOUBLE NOT NULL DEFAULT 0,
    amount_paid DOUBLE NOT NULL DEFAULT 0,
    balance_amount DOUBLE NOT NULL DEFAULT 0,
    receipt_id VARCHAR(50) DEFAULT NULL,
    transaction_id VARCHAR(50) DEFAULT NULL,
    CONSTRAINT fk_payment_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_details(candidate_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_pg FOREIGN KEY (pg_id) REFERENCES pg(id) ON DELETE CASCADE
);

-- ======================================================
-- Basic SELECT queries for reference
-- ======================================================
SELECT * FROM pg;
SELECT * FROM owner;
SELECT * FROM candidate_details;
SELECT * FROM deleted_candidates;
SELECT * FROM payment_history;

-- ======================================================
-- DELETE all data (careful: this removes everything)
-- ======================================================
DELETE FROM pg;
DELETE FROM owner;
DELETE FROM candidate_details;
DELETE FROM deleted_candidates;
DELETE FROM payment_history;

-- ======================================================
-- Check table structure
-- ======================================================
DESCRIBE deleted_candidates;

-- ======================================================
-- Fetch all PGs with owner info
-- ======================================================
SELECT 
    pg.id AS pg_id,
    pg.pg_name,
    pg.address,
    pg.mobile,
    pg.email,
    pg.monthly_rent,
    owner.owner_name,
    owner.email AS owner_email,
    owner.mobile AS owner_mobile
FROM pg
JOIN owner ON pg.owner_id = owner.id
WHERE pg.is_deleted = b'0';

-- ======================================================
-- Fetch all candidates with PG info
-- ======================================================
SELECT 
    c.candidate_id,
    c.name,
    c.gender,
    c.age,
    c.room_no,
    c.mobile,
    c.email,
    c.joining_date,
    pg.pg_name,
    pg.address AS pg_address
FROM candidate_details c
JOIN pg ON c.pg_id = pg.id
WHERE c.deleted = b'0';

-- ======================================================
-- Fetch deleted candidates
-- ======================================================
SELECT 
    d.id,
    d.candidate_id,
    d.name,
    d.gender,
    d.room_no,
    d.deleted_at
FROM deleted_candidates d
ORDER BY d.deleted_at DESC;

-- ======================================================
-- Fetch payment history for a specific candidate
-- Replace 123 with candidate_id
-- ======================================================
SELECT 
    ph.id,
    ph.payment_date,
    ph.amount_paid,
    ph.advance_amount,
    ph.balance_amount,
    ph.payment_month,
    ph.payment_year,
    ph.status,
    ph.payment_method,
    pg.pg_name,
    c.name AS candidate_name
FROM payment_history ph
JOIN candidate_details c ON ph.candidate_id = c.candidate_id
JOIN pg ON ph.pg_id = pg.id
WHERE c.candidate_id = 123;

-- ======================================================
-- Fetch monthly payment summary for a PG
-- Replace 1 with PG id
-- ======================================================
SELECT 
    ph.payment_month,
    ph.payment_year,
    COUNT(*) AS total_payments,
    SUM(ph.amount_paid) AS total_paid,
    SUM(ph.balance_amount) AS total_balance
FROM payment_history ph
JOIN pg ON ph.pg_id = pg.id
WHERE pg.id = 1
GROUP BY ph.payment_year, ph.payment_month
ORDER BY ph.payment_year DESC, ph.payment_month DESC;

-- ======================================================
-- Fetch pending payments for all candidates
-- ======================================================
SELECT 
    c.candidate_id,
    c.name,
    ph.amount_paid,
    ph.balance_amount,
    ph.status,
    pg.pg_name
FROM payment_history ph
JOIN candidate_details c ON ph.candidate_id = c.candidate_id
JOIN pg ON ph.pg_id = pg.id
WHERE ph.status != 'PAID';

-- ======================================================
-- Fetch candidates by room number
-- Replace 'A101' with room number
-- ======================================================
SELECT 
    c.candidate_id,
    c.name,
    c.room_no,
    pg.pg_name
FROM candidate_details c
JOIN pg ON c.pg_id = pg.id
WHERE c.room_no = 'A101';

-- ======================================================
-- Search PGs by owner name
-- Replace '%John%' with owner name
-- ======================================================
SELECT 
    pg.id,
    pg.pg_name,
    pg.address,
    owner.owner_name
FROM pg
JOIN owner ON pg.owner_id = owner.id
WHERE owner.owner_name LIKE '%John%';

-- ======================================================
-- Fetch all candidates with pending rent status
-- ======================================================
SELECT 
    c.candidate_id,
    c.name,
    ph.status,
    ph.balance_amount,
    pg.pg_name
FROM candidate_details c
JOIN payment_history ph ON c.candidate_id = ph.candidate_id
JOIN pg ON c.pg_id = pg.id
WHERE ph.status != 'PAID';

-- ======================================================
-- Get total candidates per PG
-- ======================================================
SELECT 
    pg.pg_name,
    COUNT(c.candidate_id) AS total_candidates
FROM pg
LEFT JOIN candidate_details c ON pg.id = c.pg_id AND c.deleted = b'0'
GROUP BY pg.pg_name;

-- ======================================================
-- Delete all data from entire database (reset)
-- Use carefully: resets auto-increment and removes all records
-- ======================================================
SET FOREIGN_KEY_CHECKS = 0; -- Temporarily disable foreign key checks
SET SQL_SAFE_UPDATES = 0;

DELETE FROM payment_history;
DELETE FROM deleted_candidates;
DELETE FROM candidate_details;
DELETE FROM pg;
DELETE FROM owner;

ALTER TABLE payment_history AUTO_INCREMENT = 1;
ALTER TABLE deleted_candidates AUTO_INCREMENT = 1;
ALTER TABLE candidate_details AUTO_INCREMENT = 1;
ALTER TABLE pg AUTO_INCREMENT = 1;
ALTER TABLE owner AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- ======================================================
-- NOTES / SUGGESTIONS FOR FUTURE REFERENCE
-- ======================================================
-- 1. Ensure LONGBLOB fields (photo/avatar/id_proof) are handled correctly in the application to prevent large data issues.
-- 2. Use transaction management when deleting/resetting tables to prevent partial deletes.
-- 3. Consider adding indexes on frequently queried columns (e.g., candidate_id, pg_id) for faster lookups.
-- 4. ENUM fields (payment_method, status) can be changed to lookup tables for flexibility in future.
-- 5. Always check foreign key relationships when inserting/updating candidates, PGs, and owners.
-- 6. Soft delete is used for pg (is_deleted) and candidate_details (deleted). Ensure application logic respects these flags.
