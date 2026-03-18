CREATE DATABASE IF NOT EXISTS parking_db;

USE parking_db;

-- Table will be auto-created by Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- But if needed manually, it looks like this:
-- CREATE TABLE vehicle (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     license_plate VARCHAR(255) NOT NULL,
--     entry_time DATETIME,
--     exit_time DATETIME,
--     status VARCHAR(50)
-- );
