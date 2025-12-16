CREATE TABLE owner (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  email VARCHAR(100),
  mobile VARCHAR(15),
  password_hash VARCHAR(255)
);

CREATE TABLE pg (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pg_name VARCHAR(100),
  address VARCHAR(255),
  mobile VARCHAR(15),
  email VARCHAR(100),
  owner_id BIGINT,
  CONSTRAINT fk_pg_owner FOREIGN KEY (owner_id) REFERENCES owner(id)
);

CREATE TABLE candidate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  gender VARCHAR(10),
  age INT,
  dob DATE,
  mobile VARCHAR(15),
  email VARCHAR(100),
  room_no VARCHAR(20),
  photo_path VARCHAR(255),
  aadhaar VARCHAR(20),
  joining_date DATE,
  guardian_mobile VARCHAR(15),
  pg_id BIGINT,
  CONSTRAINT fk_candidate_pg FOREIGN KEY (pg_id) REFERENCES pg(id)
);
